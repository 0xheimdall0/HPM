import com.password4j.Argon2Function;
import com.password4j.Hash;
import com.password4j.types.Argon2;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;

public class CryptoTest {
    public static void main(String[] args) throws Exception {

        // List to hold all the entries (PasswordEntry objects)
        List<PasswordEntry> entries = new ArrayList<>();

        // Add test entries
        entries.add(new PasswordEntry("Gmail", "test@gmail.com", "Test123!"));
        entries.add(new PasswordEntry("GitHub", "Test123", "MyPassword"));

        for (PasswordEntry elt : entries) {
            System.out.println(elt.label + " / " + elt.username);
        }

        String message = "My Secret!";

        // Create a key in a format accepted by AES, according to a master password
        String masterPassword = "hunter2";
        String salt = "myFixedSaltForNow";
        Argon2Function argon2 = Argon2Function.getInstance(65536, 3, 4, 32, Argon2.ID);
        Hash hash = argon2.hash(masterPassword, salt);
        byte[] keyBytes = hash.getBytes();
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

        // Creates a random 12 byte number
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);

        // Creates the "encrypt/decrypt machine"
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Sets the "machine" to "encrypt", encrypts the message and spits out the random bytes
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encrypted = cipher.doFinal(message.getBytes());

        // Save logic combines the nonce required to decrypt and the encrypted hash
        byte[] combined = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, combined, 0, nonce.length);
        System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);

        // Write the combination to the vault
        Path file = Path.of("vault.dat");
        Files.write(file, combined);

        // Read from the vault
        byte[] fromFile = Files.readAllBytes(file);

        // Load and split the data
        byte[] loadedNonce = Arrays.copyOfRange(fromFile, 0, nonce.length);
        byte[] cipherText = Arrays.copyOfRange(fromFile, nonce.length, fromFile.length);
        GCMParameterSpec loadedSpec = new GCMParameterSpec(128, loadedNonce);

        // Sets the "machine" to "decrypt", spits out the result
        cipher.init(Cipher.DECRYPT_MODE, key, loadedSpec);
        byte[] decrypted = cipher.doFinal(cipherText);

        // Output test
        System.out.println("Encrypted: " + Base64.getEncoder().encodeToString(encrypted));
        System.out.println("Decrypted: " + new String(decrypted));
    }
}
