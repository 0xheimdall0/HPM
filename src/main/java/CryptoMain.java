import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CryptoMain {
    public static void main(String[] args) throws Exception {
        Path file = Path.of("vault.dat");

        // Scanner initialisation
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please provide the master password: ");
        String masterPassword = scanner.nextLine();

        // List to load and hold all the entries (PasswordEntry objects)
        List<PasswordEntry> entries = LoadLogic.load(masterPassword);

        // Program runs until we don't want it to anymore
        menu:
        while (true) {
            // Menu
            System.out.println("\n(1) - List entries");
            System.out.println("(2) - Add an entry");
            System.out.println("(3) - Generate a password");
            System.out.println("(4) - Save and exit");
            System.out.print("Please choose: ");
            String choice = scanner.nextLine();

            // Actions
            switch (choice) {
                case "1":
                    for (PasswordEntry elt : entries) {
                        System.out.println(elt.label);
                    }
                    break;
                case "2":
                    System.out.println("Enter label (site/app): ");
                    String label = scanner.nextLine();
                    System.out.println("Enter mail or username: ");
                    String username = scanner.nextLine();
                    String password;
                    int length1;
                    while (true) {
                        System.out.println("Do you want to generate a password? (Y/N): ");
                        String gen = scanner.nextLine();
                        if (gen.equals("y") || gen.equals("Y")) {
                            System.out.println("What should be the length of the password? (12-36): ");
                            while (true) {
                                System.out.println("Enter the password's length: ");
                                length1 = scanner.nextInt();
                                scanner.nextLine();
                                if (length1 < 12 || length1 > 36) {
                                    System.out.println("Please respect the required length.");
                                } else {
                                    password = PasswordGenerator.generatePassword(length1);
                                    break;
                                }
                            }
                            break;
                        } else if (gen.equals("n") || gen.equals("N")) {
                            System.out.println("Enter the password: ");
                            password = scanner.nextLine();
                            break;
                        } else {
                            System.out.println("Please provide a valid answer.");
                        }
                    }
                    entries.add(new PasswordEntry(label, username, password));
                    System.out.println("Entry added!");
                    break;
                case "3":
                    while (true) {
                        System.out.println("Enter the password's length: ");
                        int length2 = scanner.nextInt();
                        scanner.nextLine();
                        if (length2 < 12 || length2 > 36) {
                            System.out.println("Please respect the required length.");
                        } else {
                            System.out.println(PasswordGenerator.generatePassword(length2));
                            break;
                        }
                    }
                    break;

                case "4":
                    SaveLogic.save(entries, masterPassword);
                    System.out.println("Saved. See you!");
                    break menu;
                default:
                    System.out.println("Please provide a valid answer");
            }
        }
    }
}
