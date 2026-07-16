import com.password4j.Argon2Function;
import com.password4j.Hash;
import com.password4j.types.Argon2;

import javax.crypto.spec.SecretKeySpec;

public class DeriveKey {
    protected static SecretKeySpec deriveKey(String masterPassword) {
        String salt = "myFixedSaltForNow";
        Argon2Function argon2 = Argon2Function.getInstance(65536, 3, 4, 32, Argon2.ID);
        Hash hash = argon2.hash(masterPassword, salt);
        byte[] keyBytes = hash.getBytes();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
