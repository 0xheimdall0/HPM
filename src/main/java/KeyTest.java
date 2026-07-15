import com.password4j.Argon2Function;
import com.password4j.Hash;
import com.password4j.types.Argon2;

public class KeyTest {
    public static void main(String[] args) {

        String masterPassword = "Pamplemousse321!";
        String fixedTestSalt = "testFixedSalt";

        // Creates the "machine" with memory used in KB, amt of passes over the data, amt of threads,
        // output size in bytes and the Argon variant
        Argon2Function argon2 = Argon2Function.getInstance(65536, 3, 4, 32, Argon2.ID);
        Hash hash = argon2.hash(masterPassword, fixedTestSalt);

        byte[] keyBytes = hash.getBytes();
        System.out.println("Key length: " + keyBytes.length);

    }
}
