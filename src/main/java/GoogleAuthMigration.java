import org.apache.commons.codec.binary.Base32;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class GoogleAuthMigration {
    public record Account(String name, String issuer, String base32Secret) {}

    static List<Account> parse(String migrationUrl) throws Exception {
        // Pull out the data
        int idx = migrationUrl.indexOf("data=");
        if (idx < 0) throw new Exception("Not a Google Auth migration QR.");
        String dataParam = migrationUrl.substring(idx + 5);
        int amp = dataParam.indexOf("&");
        if (amp >= 0) dataParam = dataParam.substring(0, amp);

        // URL decode, base64 decode amd then raw protobuf bytes
        String base64 = URLDecoder.decode(dataParam, StandardCharsets.UTF_8);
        byte[] payload = Base64.getDecoder().decode(base64);

        // Collect accounts
        List<Account> accounts = new ArrayList<>();
        int[] pos = {0};
        while (pos[0] < payload.length) {
            long tag = readVarint(payload, pos);
            int field = (int) (tag >> 3);
            int wireType = (int) (tag & 0x7);
            if (field == 1 && wireType == 2) {
                int len = (int) readVarint(payload, pos);
                if (len < 0 || pos[0] + len > payload.length) throw new Exception("Malformed migration data");
                byte[] sub = Arrays.copyOfRange(payload, pos[0], pos[0] + len);
                pos[0] += len;
                accounts.add(parseAccount(sub));
            } else {
                skipField(payload, pos, wireType);
            }
        }
        return accounts;
    }

    private static Account parseAccount(byte[] data) {
        int[] pos = {0};
        byte[] secret = new byte[0];
        String name = "", issuer = "";
        while (pos[0] < data.length) {
            long tag = readVarint(data, pos);
            int field = (int) (tag >> 3);
            int wireType = (int) (tag & 0x7);
            if (wireType == 2) {
                int len = (int) readVarint(data, pos);
                if (len < 0 || pos[0] + len > data.length) throw new RuntimeException("Malformed migration data");
                byte[] value = Arrays.copyOfRange(data, pos[0], pos[0] + len);
                pos[0] += len;
                switch (field) {
                    case 1 -> secret = value;
                    case 2 -> name = new String(value, StandardCharsets.UTF_8);
                    case 3 -> issuer = new String(value, StandardCharsets.UTF_8);
                }
            } else {
                skipField(data, pos, wireType);
            }
        }
        String base32 = new Base32().encodeToString(secret).replace("=", "");
        return new Account(name, issuer, base32);
    }

    private static long readVarint(byte[] data, int[] pos) {
        long result = 0;
        int shift = 0;
        while (true) {
            if (pos[0] >= data.length) throw new RuntimeException("Malformed migration data");
            byte b = data[pos[0]++];
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return result;
    }

    private static void skipField(byte[] data, int[] pos, int wireType) {
        switch(wireType) {
            case 0 -> readVarint(data, pos);
            case 2 -> {
                int len = (int) readVarint(data, pos);
                if (len < 0 || pos[0] + len > data.length) throw new RuntimeException("Malformed migration data");
                pos[0] += len;
            }
            case 5 -> pos[0] += 4;
            case 1 -> pos[0] += 8;
            default -> throw new RuntimeException("Unknown wire type: " + wireType);
        }
    }
}
