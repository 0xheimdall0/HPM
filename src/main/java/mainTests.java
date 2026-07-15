import com.password4j.Hash;
import com.password4j.Password;

public class mainTests {
    public static void main(String[] args) {
        Hash hashTest = Password.hash("TestPassword123!").addRandomSalt().withArgon2();
        System.out.println("Hash: " + hashTest);
    }
}