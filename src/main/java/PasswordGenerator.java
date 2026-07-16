import java.security.SecureRandom;

public class PasswordGenerator {
    protected static String generatePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@+%&/()=?![]{}-_<>";

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }
}
