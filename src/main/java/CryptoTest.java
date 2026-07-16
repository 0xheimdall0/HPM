import com.password4j.Argon2Function;
import com.password4j.Hash;
import com.password4j.types.Argon2;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

import java.nio.file.Files;
import java.nio.file.Path;

public class CryptoTest {
    public static void main(String[] args) throws Exception {

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

        // Creates a path variable and writes the encrypted bytes to the file
        Path file = Path.of("vault.dat");
        Files.write(file, encrypted);

        byte[] fromFile = Files.readAllBytes(file);
        System.out.println("Encrypted: " + Base64.getEncoder().encodeToString(encrypted));

        // Sets the "machine" to "decrypt", spits out the result
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] decrypted = cipher.doFinal(fromFile);
        System.out.println("Decrypted: " + new String(decrypted));
    }
}
