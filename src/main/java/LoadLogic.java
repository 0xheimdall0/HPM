import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class LoadLogic {
    protected static VaultData load(char[] masterPassword) throws Exception {
        Path file = Path.of("vault.dat");

        // If there is no vault yet, new salt + key, empty entries
        if (!Files.exists(file) || Files.size(file) == 0) {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            SecretKeySpec key = DeriveKey.deriveKey(masterPassword, salt);
            return new VaultData(new ArrayList<>(), key, salt);
        }

        byte[] fromFile = Files.readAllBytes(file);
        byte version = fromFile[0];
        if (version != 1) throw new Exception("Unsupported vault version: " + version);

        byte[] loadedSalt = Arrays.copyOfRange(fromFile, 1, 17);
        byte[] loadedNonce = Arrays.copyOfRange(fromFile, 17, 29);
        byte[] cipherText = Arrays.copyOfRange(fromFile, 29, fromFile.length);

        SecretKeySpec key = DeriveKey.deriveKey(masterPassword, loadedSalt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, loadedNonce));
        byte[] decrypted = cipher.doFinal(cipherText);

        Type listType = new TypeToken<List<PasswordEntry>>(){}.getType();
        List<PasswordEntry> loadedEntries = new Gson().fromJson(new String(decrypted), listType);

        return new VaultData(loadedEntries, key, loadedSalt);
    }
}
