import java.util.ArrayList;
import java.util.List;

public class CryptoMain {
    public static void main(String[] args) throws Exception {
        // List to hold all the entries (PasswordEntry objects)
        List<PasswordEntry> entries = new ArrayList<>();

        // Add test entries
        entries.add(new PasswordEntry("Gmail", "test@gmail.com", "Test123!"));
        entries.add(new PasswordEntry("GitHub", "Test123", "MyPassword"));

        // Master password and save
        String masterPassword = "hunter2";
        SaveLogic.save(entries, masterPassword);

        // Load and test print
        List<PasswordEntry> loadedEntries = LoadLogic.load(masterPassword);
        for (PasswordEntry e : loadedEntries) {
            System.out.println("Loaded: " + e.label + " / " + e.username);
        }
    }
}
