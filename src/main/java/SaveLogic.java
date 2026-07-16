import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;

public class SaveLogic {
    protected static void save(List<PasswordEntry> entries, String masterPassword) throws Exception {
        // Turns the entries into a single string that can be easily encrypted
        StringBuilder sb = new StringBuilder();
        for (PasswordEntry elt : entries) {
            sb.append(elt.label).append(",").append(elt.username).append(",").append(elt.password).append("\n");
        }
        String plainText = sb.toString();
        System.out.println(plainText);

        // Creates a random 12 byte number
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        // Create the key and the spec
        SecretKeySpec key = DeriveKey.deriveKey(masterPassword);
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);

        // Creates the "encrypt/decrypt machine"
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Sets the "machine" to "encrypt", encrypts the message and spits out the random bytes
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());

        // Save logic combines the nonce required to decrypt and the encrypted hash
        byte[] combined = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, combined, 0, nonce.length);
        System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);

        // Write the combination to the vault
        Path file = Path.of("vault.dat");
        Files.write(file, combined);
    }
}
