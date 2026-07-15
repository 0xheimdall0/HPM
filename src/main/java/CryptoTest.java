import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class CryptoTest {
    public static void main(String[] args) throws Exception {

        String message = "My Secret!";

        // Create a fake test key that's in a format accepted by AES
        byte[] keyBytes = new byte[32];
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
        System.out.println("Encrypted: " + Base64.getEncoder().encodeToString(encrypted));

        // Sets the "machine" to "decrypt", spits out the result
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] decrypted = cipher.doFinal(encrypted);
        System.out.println("Decrypted: " + new String(decrypted));

    }
}
