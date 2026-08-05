import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.List;

public class SaveLogic {
    protected static void save(List<PasswordEntry> entries, SecretKeySpec key, byte[] salt) throws Exception {
        // Turns the entries into a single Json string that can be easily encrypted
        String plainText = new com.google.gson.Gson().toJson(entries);

        // Creates a random 12 byte number
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);

        // Creates the "encrypt/decrypt machine"
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Sets the "machine" to "encrypt", encrypts the message and spits out the random bytes
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] plainBytes = plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] encrypted = cipher.doFinal(plainBytes);
        java.util.Arrays.fill(plainBytes, (byte) 0);

        // Save logic combines the nonce required to decrypt and the encrypted hash
        byte VERSION = 1;
        byte[] combined = new byte[1 + nonce.length + encrypted.length + salt.length];
        combined[0] = VERSION;
        System.arraycopy(salt, 0, combined, 1, salt.length);
        System.arraycopy(nonce, 0, combined, 1 + salt.length, nonce.length);
        System.arraycopy(encrypted, 0, combined, 1 + salt.length + nonce.length, encrypted.length);

        // Write the combination to the vault, restricting access before it ever lands at its final name
        Path file = Path.of("vault.dat");
        Path temp = Path.of("vault.dat.tmp");
        Files.write(temp, combined);
        restrictPermissions(temp);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    static void restrictPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | java.io.IOException e) {
            java.io.File f = path.toFile();
            f.setReadable(false, false);  f.setReadable(true, true);
            f.setWritable(false, false);  f.setWritable(true, true);
        }
    }
}
