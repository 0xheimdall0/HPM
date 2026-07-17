import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoadLogic {
    protected static List<PasswordEntry> load(String masterPassword) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        Path file = Path.of("vault.dat");

        // Check whether vault.dat is empty
        if (!Files.exists(file) || Files.size(file) == 0) {
            return new ArrayList<>();
        }

        // Read from the vault
        byte[] fromFile = Files.readAllBytes(file);

        // Load and split the data
        byte[] loadedSalt = Arrays.copyOfRange(fromFile, 0, 16);
        byte[] loadedNonce = Arrays.copyOfRange(fromFile, 16, 28);
        byte[] cipherText = Arrays.copyOfRange(fromFile, 28, fromFile.length);
        GCMParameterSpec loadedSpec = new GCMParameterSpec(128, loadedNonce);

        // Setup the key
        SecretKeySpec key = DeriveKey.deriveKey(masterPassword, loadedSalt);

        // Sets the "machine" to "decrypt", spits out the result
        cipher.init(Cipher.DECRYPT_MODE, key, loadedSpec);
        byte[] decrypted = cipher.doFinal(cipherText);

        String decryptedText = new String(decrypted);

        // Split the decrypted text into separate strings
        String[] elements = decryptedText.split("\n");

        // Load the entries in the adequate format
        List<PasswordEntry> loadedEntries = new ArrayList<>();
        for (String elt : elements) {
            String[] values = elt.split(",");
            String label = values[0];
            String username = values[1];
            String password = values[2];
            loadedEntries.add(new PasswordEntry(label, username, password));
        }
        return loadedEntries;
    }
}
