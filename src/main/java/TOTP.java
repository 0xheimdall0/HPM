import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

public class TOTP {
    static String generateCode(String base32Secret) throws Exception {
        byte[] key = new Base32().decode(base32Secret.replace(" ", "").toUpperCase());
        long timeStep = System.currentTimeMillis() / 1000L / 30;
        byte[] counter = ByteBuffer.allocate(8).putLong(timeStep).array();

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(counter);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int OTP = binary % 1000000;
        return String.format("%06d", OTP);
    }
}
