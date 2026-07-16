import com.password4j.Hash;
import com.password4j.Password;

public class mainTests {
    public static void main(String[] args) {
        PasswordEntry entry = new PasswordEntry("Gmail", "test@gmail.com", "Test123!");
        System.out.println(entry.label + " / " + entry.username + " / " + entry.password);
    }
}