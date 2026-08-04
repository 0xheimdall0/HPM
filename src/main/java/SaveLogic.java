import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
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
        byte[] encrypted = cipher.doFinal(plainText.getBytes());

        // Save logic combines the nonce required to decrypt and the encrypted hash
        byte[] combined = new byte[nonce.length + encrypted.length + salt.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(nonce, 0, combined, salt.length, nonce.length);
        System.arraycopy(encrypted, 0, combined, salt.length + nonce.length, encrypted.length);

        // Write the combination to the vault
        Files.write(Path.of("vault.dat"), combined);
    }
}
