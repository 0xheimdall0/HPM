public class PasswordEntry {
    // The pieces of data needed to form an entry
    String label;
    String username;
    String password;

    // Constructor for the final entry
    public PasswordEntry(String label, String username, String password) {
        this.label = label;
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        return label + " - " + username;
    }
}
