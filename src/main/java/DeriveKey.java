import com.password4j.Argon2Function;
import com.password4j.Hash;
import com.password4j.types.Argon2;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class DeriveKey {
    protected static SecretKeySpec deriveKey(String masterPassword, byte[] salt) {
        Argon2Function argon2 = Argon2Function.getInstance(65536, 3, 4, 32, Argon2.ID);
        String saltStr = Base64.getEncoder().encodeToString(salt);
        Hash hash = argon2.hash(masterPassword, saltStr);
        return new SecretKeySpec(hash.getBytes(), "AES");
    }
}
