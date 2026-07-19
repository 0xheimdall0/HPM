import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class HPMUI {
    public static void main(String[] args) {
        // Initiate the frame
        JFrame frame = new JFrame("HPM - Heimdall's password manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);
        frame.setLayout(new java.awt.FlowLayout());

        // Buttons
        JButton generatePWD = new JButton("Generate Password");
        JButton unlockBtn = new JButton("Unlock");
        JButton addEntry = new JButton("Add an entry");
        JButton deleteEntry = new JButton("Delete");
        JButton seePWD = new JButton("Show password");
        JButton copyPWD = new JButton("Copy password");
        JButton lockBtn = new JButton("Lock");
        JButton editBtn = new JButton("Edit");

        // Fields
        JTextField outputPWD = new JTextField(37);
        JPasswordField passwordField = new JPasswordField(36);

        // Output fields
        DefaultListModel<PasswordEntry> listModel = new DefaultListModel<>();
        JList<PasswordEntry> entriesDisplay = new JList<>(listModel);

        // Add the items to the frame
        frame.add(generatePWD);
        frame.add(outputPWD);
        frame.add(passwordField);
        frame.add(unlockBtn);
        frame.add(addEntry);
        frame.add(deleteEntry);
        frame.add(seePWD);
        frame.add(copyPWD);
        frame.add(lockBtn);
        frame.add(editBtn);
        frame.add(new JScrollPane(entriesDisplay));
        addEntry.setEnabled(false);
        deleteEntry.setEnabled(false);
        seePWD.setEnabled(false);
        copyPWD.setEnabled(false);
        lockBtn.setEnabled(false);
        editBtn.setEnabled(false);

        // Run the password generating code when the button is clicked
        generatePWD.addActionListener(e -> {
            String PWD = PasswordGenerator.generatePassword(16);
            outputPWD.setText(PWD);
        });

        // Password unlock code
        List<PasswordEntry> entries = new ArrayList<>();

        unlockBtn.addActionListener(e -> {
            String masterPassword = new String(passwordField.getPassword());
            try {
                entries.clear();
                entries.addAll(LoadLogic.load(masterPassword));
                refreshList(listModel, entries);
                JOptionPane.showMessageDialog(frame, "Unlocked successfully! " + entries.size() + " entries loaded.");
                addEntry.setEnabled(true);
                deleteEntry.setEnabled(true);
                seePWD.setEnabled(true);
                copyPWD.setEnabled(true);
                lockBtn.setEnabled(true);
                editBtn.setEnabled(true);
                unlockBtn.setEnabled(false);
            } catch (Exception err) {
                JOptionPane.showMessageDialog(frame, "Wrong password!");
            }
        });

        // Add entries button manager
        addEntry.addActionListener(e -> {
            String label = JOptionPane.showInputDialog("Label (site/app): ");
            String username = JOptionPane.showInputDialog("Usename: ");
            if (label == null || username == null) return;
            int choice = JOptionPane.showConfirmDialog(frame, "Generate password randomly?",
                    "Password", JOptionPane.YES_NO_OPTION);
            String password;
            if (choice == JOptionPane.YES_OPTION) {
                password = PasswordGenerator.generatePassword(16);
            } else {
                password = JOptionPane.showInputDialog("Password: ");
            }
            entries.add(new PasswordEntry(label, username, password));
            autoSave(entries, passwordField, frame);
            refreshList(listModel, entries);
        });

        // Delete entries button manager
        deleteEntry.addActionListener(e -> {
            if (entries.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No entry to delete!");
                return;
            }

            // Make the list of choices to delete the right entry
            String[] labels = new String[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                labels[i] = entries.get(i).label + " - " + entries.get(i).username;
            }

            String selected = (String) JOptionPane.showInputDialog(frame, "Which entry do you wish to delete?",
                    "Delete", JOptionPane.QUESTION_MESSAGE,
                    null, labels, labels[0]);
            if (selected == null) return;
            int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete\"" + selected + "\"?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.NO_OPTION) return;
            int index = java.util.Arrays.asList(labels).indexOf(selected);
            entries.remove(index);
            refreshList(listModel, entries);
            autoSave(entries, passwordField, frame);
        });

        // Edit button manager
        editBtn.addActionListener(e -> {
            PasswordEntry selected = entriesDisplay.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame,"No entry is selected.");
                return;
            }
            String newLabel = JOptionPane.showInputDialog("Label: ", selected.label);
            if (newLabel == null) return;
            String newUsername = JOptionPane.showInputDialog("Username: ", selected.username);
            if (newUsername == null) return;
            String newPassword = JOptionPane.showInputDialog("Password: ", selected.password);
            if (newPassword == null) return;

            selected.label = newLabel;
            selected.username = newUsername;
            selected.password = newPassword;

            refreshList(listModel, entries);
            autoSave(entries, passwordField, frame);
        });

        // See password button manager
        seePWD.addActionListener(e -> {
            PasswordEntry selected = entriesDisplay.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame,"No entry is selected.");
                return;
            }
            JOptionPane.showMessageDialog(frame, "The password is: " + selected.password);
        });

        // Copy password button manager
        copyPWD.addActionListener(e -> {
            PasswordEntry selected = entriesDisplay.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame, "No entry is selected.");
                return;
            }
            StringSelection data = new StringSelection(selected.password);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, null);
            JOptionPane.showMessageDialog(frame, "Password copied to clipboard!");
        });

        // Lock button manager
        lockBtn.addActionListener(e -> {
            // Clear everything
            entries.clear();
            listModel.clear();
            passwordField.setText("");

            // Lock all unneeded buttons
            addEntry.setEnabled(false);
            deleteEntry.setEnabled(false);
            seePWD.setEnabled(false);
            copyPWD.setEnabled(false);
            lockBtn.setEnabled(false);
            editBtn.setEnabled(false);
            unlockBtn.setEnabled(true);
        });

        // Sets the window visible only once everything is ready to be shown
        frame.setVisible(true);
    }

    // Method to show all the entries labels and usernames
    protected static void refreshList(DefaultListModel<PasswordEntry> model, List<PasswordEntry> entries) {
        model.clear();
        for (PasswordEntry elt : entries) {
            model.addElement(elt);
        }
    }

    // Auto save method
    protected static void autoSave(List<PasswordEntry> entries, JPasswordField passwordField, JFrame frame) {
        try {
            SaveLogic.save(entries, new String(passwordField.getPassword()));
        } catch (Exception err) {
            JOptionPane.showMessageDialog(frame, "Data could not be saved.");
        }
    }
}
