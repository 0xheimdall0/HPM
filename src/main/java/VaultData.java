import javax.crypto.spec.SecretKeySpec;
import java.util.List;

public record VaultData(List<PasswordEntry> entries, SecretKeySpec key, byte[] salt) {}