import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.tools.Tool;

public class HPMUI {
    public static void main(String[] args) {
        com.formdev.flatlaf.FlatDarculaLaf.setup();

        // Initiate the frame
        JFrame frame = new JFrame("HPM - Heimdall's password manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);
        frame.setLayout(new java.awt.BorderLayout());
        frame.setLocationRelativeTo(null);
        ((JComponent) frame.getContentPane()).setBorder(
                javax.swing.BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Buttons
        JButton generatePWD = new JButton("Generate Password");
        JButton unlockBtn = new JButton("Unlock");
        JButton addEntry = new JButton("Add");
        JButton deleteEntry = new JButton("Delete");
        JButton seePWD = new JButton("Show password");
        JButton copyPWD = new JButton("Copy password");
        JButton lockBtn = new JButton("Lock");
        JButton editBtn = new JButton("Edit");

        // Fields
        JTextField outputPWD = new JTextField(24);
        JPasswordField passwordField = new JPasswordField(24);

        // Output fields
        DefaultListModel<PasswordEntry> listModel = new DefaultListModel<>();
        JList<PasswordEntry> entriesDisplay = new JList<>(listModel);

        // Top panel
        JPanel topPanel = new JPanel();
        topPanel.add(passwordField);
        topPanel.add(unlockBtn);
        topPanel.add(lockBtn);
        frame.add(topPanel, java.awt.BorderLayout.NORTH);

        // Center items
        frame.add(new JScrollPane(entriesDisplay), BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addEntry);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteEntry);
        buttonPanel.add(seePWD);
        buttonPanel.add(copyPWD);
        buttonPanel.add(generatePWD);
        buttonPanel.add(outputPWD);
        frame.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        // Set the protected items to disabled
        addEntry.setEnabled(false);
        editBtn.setEnabled(false);
        deleteEntry.setEnabled(false);
        seePWD.setEnabled(false);
        copyPWD.setEnabled(false);
        lockBtn.setEnabled(false);

        // Run the password generating code when the button is clicked
        generatePWD.addActionListener(e -> {
            String PWD = PasswordGenerator.generatePassword(16);
            outputPWD.setText(PWD);
        });

        // Send password with enter key
        passwordField.addActionListener(e -> unlockBtn.doClick());

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
            if (label == null) return;
            else if (label.isBlank()) {
                JOptionPane.showMessageDialog(frame, "Every field is required.");
                return;
            };
            String username = JOptionPane.showInputDialog("Username: ");
            if (username == null) return;
            else if (username.isBlank()) {
                JOptionPane.showMessageDialog(frame, "Every field is required.");
                return;
            };
            int choice = JOptionPane.showConfirmDialog(frame, "Generate password randomly?",
                    "Password", JOptionPane.YES_NO_OPTION);
            String password;
            if (choice == JOptionPane.YES_OPTION) {
                password = PasswordGenerator.generatePassword(16);
            } else {
                password = JOptionPane.showInputDialog("Password: ");
                if (password == null) return;
                else if (password.isBlank()) {
                    JOptionPane.showMessageDialog(frame, "Every field is required.");
                    return;
                }
            }
            entries.add(new PasswordEntry(label, username, password));
            autoSave(entries, passwordField, frame);
            refreshList(listModel, entries);
        });

        // Delete entries button manager
        deleteEntry.addActionListener(e -> {
            PasswordEntry selected = entriesDisplay.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame,"No entry is selected");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Do you really want to delete \"" + selected + "\"?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.NO_OPTION) return;
            entries.remove(selected);
            refreshList(listModel, entries);
            autoSave(entries, passwordField, frame);
        });

        // Edit button manager
        editBtn.addActionListener(e -> {
            PasswordEntry selected = entriesDisplay.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame, "No entry is selected.");
                return;
            }
            JTextField labelField = new JTextField(selected.label, 20);
            JTextField usernameField = new JTextField(selected.username, 20);
            JPasswordField passwordFieldEdit = new JPasswordField(selected.password, 20);
            JCheckBox showPWD = new JCheckBox("Show password");

            // Show / hide password
            char maskChar = passwordFieldEdit.getEchoChar();
            showPWD.addActionListener(ev -> {
                if (showPWD.isSelected()) passwordFieldEdit.setEchoChar((char) 0);
                else passwordFieldEdit.setEchoChar(maskChar);
            });

            // Stack every items vertically in the edit panel
            JPanel editPanel = new JPanel(new java.awt.GridLayout(0, 1, 5, 5));
            editPanel.add(new JLabel("Label:"));
            editPanel.add(labelField);
            editPanel.add(new JLabel("Username/email:"));
            editPanel.add(usernameField);
            editPanel.add(new JLabel("Password:"));
            editPanel.add(passwordFieldEdit);
            editPanel.add(showPWD);

            // Show panel in one dialog with OK / cancel options
            int result = JOptionPane.showConfirmDialog(frame, editPanel, "Edit entry", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) return;

            String newLabel = labelField.getText();
            String newUsername = usernameField.getText();
            String newPassword = new String(passwordField.getPassword());

            if (newLabel.isBlank() || newUsername.isBlank() || newPassword.isBlank()) {
                JOptionPane.showMessageDialog(frame, "All fields are required.");
                return;
            }

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

            String copied = selected.password;
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(copied), null);

            // Set timer for auto deletion of the clipboard
            javax.swing.Timer timer = new javax.swing.Timer(20000, ev -> {
                try {
                    String current = (String) clipboard.getData(DataFlavor.stringFlavor);
                    if (current.equals(copied)) {
                        clipboard.setContents(new StringSelection(""), null);
                    }
                } catch (Exception err) {
                    // Leave clipboard alone
                }
            });
            timer.setRepeats(false);
            timer.start();
            JOptionPane.showMessageDialog(frame, "Password copied to clipboard! Clipboard will be automatically cleared in 20 seconds.");
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
        passwordField.requestFocusInWindow();
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
