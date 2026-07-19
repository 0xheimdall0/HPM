import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.tools.JavaCompiler;
import javax.tools.Tool;

public class HPMUI {
    public static void main(String[] args) {
        com.formdev.flatlaf.FlatDarculaLaf.setup();
        PWDGenOptions opt = new PWDGenOptions();
        final String[] sessionPassword = {null};

        // Initiate the frame
        JFrame frame = new JFrame("HPM - Heimdall's password manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 600);
        frame.setLayout(new java.awt.BorderLayout());
        frame.setLocationRelativeTo(null);
        ((JComponent) frame.getContentPane()).setBorder(
                javax.swing.BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Buttons
        JButton unlockBtn = new JButton("Unlock");
        JButton addEntry = new JButton("Add");
        JButton deleteEntry = new JButton("Delete");
        JButton seePWD = new JButton("Show password");
        JButton copyPWD = new JButton("Copy password");
        JButton lockBtn = new JButton("Lock");
        JButton editBtn = new JButton("Edit");

        // Fields
        JPasswordField passwordField = new JPasswordField(24);
        JTextField searchField = new JTextField(24);

        // Output fields
        DefaultListModel<PasswordEntry> listModel = new DefaultListModel<>();
        JList<PasswordEntry> entriesDisplay = new JList<>(listModel);

        // Top panel
        JPanel topPanel = new JPanel();
        topPanel.add(passwordField);
        topPanel.add(unlockBtn);
        topPanel.add(lockBtn);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addEntry);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteEntry);
        buttonPanel.add(seePWD);
        buttonPanel.add(copyPWD);

        // Panels layout
        JPanel vaultPanel = new JPanel(new java.awt.BorderLayout());
        vaultPanel.add(topPanel, BorderLayout.NORTH);
        JPanel listArea = new JPanel(new java.awt.BorderLayout());
        listArea.add(searchField, java.awt.BorderLayout.NORTH);
        listArea.add(new JScrollPane(entriesDisplay), java.awt.BorderLayout.CENTER);
        vaultPanel.add(listArea, java.awt.BorderLayout.CENTER);
        vaultPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Generator panel
        JSpinner lengthSpinner = new JSpinner(new SpinnerNumberModel(opt.length, 4, 64, 1));
        JCheckBox lowerBox     = new JCheckBox("Lowercase (a-z)", opt.useLower);
        JCheckBox upperBox     = new JCheckBox("Uppercase (A-Z)", opt.useUpper);
        JCheckBox numbersBox     = new JCheckBox("Digits (0-9)", opt.useNumbers);
        JCheckBox symbolBox    = new JCheckBox("Symbols (!@#...)", opt.useSymbols);
        JCheckBox ambiguousBox = new JCheckBox("Exclude look-alikes", opt.excludeAmbiguous);

        JTextField genOutput = new JTextField(16);
        genOutput.setEditable(false);
        JButton doGenerateBtn = new JButton("Generate");
        JButton copyGenBtn = new JButton("Copy");

        // Read options, then generate
        doGenerateBtn.addActionListener(e -> {
            opt.length = (int) lengthSpinner.getValue();
            opt.useLower = lowerBox.isSelected();
            opt.useUpper = upperBox.isSelected();
            opt.useNumbers = numbersBox.isSelected();
            opt.useSymbols = symbolBox.isSelected();
            opt.excludeAmbiguous = ambiguousBox.isSelected();

            genOutput.setText(PasswordGenerator.generatePassword(opt));
        });

        // Copy generated password to clipboard
        copyGenBtn.addActionListener(e -> {
            AutoClearCopy(genOutput.getText(), frame);
        });

        // Generator panel layout
        JPanel genContent = new JPanel(new java.awt.GridLayout(0, 1, 5, 5));
        genContent.add(new JLabel("Length:"));
        genContent.add(lengthSpinner);
        genContent.add(lowerBox);
        genContent.add(upperBox);
        genContent.add(numbersBox);
        genContent.add(symbolBox);
        genContent.add(ambiguousBox);
        genContent.add(doGenerateBtn);
        genContent.add(genOutput);
        genContent.add(copyGenBtn);

        Runnable syncOptions = () -> {
            opt.length           = (int) lengthSpinner.getValue();
            opt.useLower         = lowerBox.isSelected();
            opt.useUpper         = upperBox.isSelected();
            opt.useNumbers        = numbersBox.isSelected();
            opt.useSymbols       = symbolBox.isSelected();
            opt.excludeAmbiguous = ambiguousBox.isSelected();
        };

        lowerBox.addActionListener(e -> syncOptions.run());
        upperBox.addActionListener(e -> syncOptions.run());
        numbersBox.addActionListener(e -> syncOptions.run());
        symbolBox.addActionListener(e -> syncOptions.run());
        ambiguousBox.addActionListener(e -> syncOptions.run());
        lengthSpinner.addChangeListener(e -> syncOptions.run());

        JPanel generatorPanel = new JPanel(new java.awt.BorderLayout());
        generatorPanel.add(genContent, java.awt.BorderLayout.NORTH);
        generatorPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Card layout
        java.awt.CardLayout cardLayout = new CardLayout();
        JPanel contentArea = new JPanel(cardLayout);
        contentArea.add(vaultPanel, "Vault");
        contentArea.add(generatorPanel, "Generator");

        // Navigation menu
        JButton navVault = new JButton("Vault");
        JButton navGen = new JButton("Generator");
        navVault.addActionListener(e -> cardLayout.show(contentArea, "Vault"));
        navGen.addActionListener(e -> cardLayout.show(contentArea, "Generator"));

        JPanel navButtons = new JPanel(new java.awt.GridLayout(0, 1, 0, 8));
        navButtons.add(navVault);
        navButtons.add(navGen);
        JPanel navPanel = new JPanel(new java.awt.BorderLayout());
        navPanel.add(navButtons, java.awt.BorderLayout.NORTH);
        navPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 10, 15));

        // Add menu on the left of the window
        frame.add(navPanel, BorderLayout.WEST);
        frame.add(contentArea, BorderLayout.CENTER);

        // Set the protected items to disabled
        addEntry.setEnabled(false);
        editBtn.setEnabled(false);
        deleteEntry.setEnabled(false);
        seePWD.setEnabled(false);
        copyPWD.setEnabled(false);
        lockBtn.setEnabled(false);

        // Send password with enter key
        passwordField.addActionListener(e -> unlockBtn.doClick());

        // Password unlock code
        List<PasswordEntry> entries = new ArrayList<>();

        unlockBtn.addActionListener(e -> {
            String masterPassword = new String(passwordField.getPassword());
            try {
                entries.clear();
                entries.addAll(LoadLogic.load(masterPassword));
                sessionPassword[0] = masterPassword;
                refreshList(listModel, entries, searchField.getText());
                JOptionPane.showMessageDialog(frame, "Unlocked successfully! " + entries.size() + " entries loaded.");
                addEntry.setEnabled(true);
                deleteEntry.setEnabled(true);
                seePWD.setEnabled(true);
                copyPWD.setEnabled(true);
                lockBtn.setEnabled(true);
                editBtn.setEnabled(true);
                unlockBtn.setEnabled(false);
                passwordField.setText("");
            } catch (Exception err) {
                JOptionPane.showMessageDialog(frame, "Wrong password!");
                passwordField.setText("");
            }
        });

        // Reactive search field
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {refreshList(listModel, entries, searchField.getText());}
            @Override
            public void removeUpdate(DocumentEvent e) {refreshList(listModel, entries, searchField.getText());}
            @Override
            public void changedUpdate(DocumentEvent e) {refreshList(listModel, entries, searchField.getText());}
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
                password = PasswordGenerator.generatePassword(opt);
            } else {
                password = JOptionPane.showInputDialog("Password: ");
                if (password == null) return;
                else if (password.isBlank()) {
                    JOptionPane.showMessageDialog(frame, "Every field is required.");
                    return;
                }
            }
            entries.add(new PasswordEntry(label, username, password));
            autoSave(entries, sessionPassword[0], frame);
            refreshList(listModel, entries, searchField.getText());
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
            if (confirm != JOptionPane.YES_OPTION) return;
            entries.remove(selected);
            refreshList(listModel, entries, searchField.getText());
            autoSave(entries, sessionPassword[0], frame);
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
            if (result != JOptionPane.OK_OPTION) return;

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

            refreshList(listModel, entries, searchField.getText());
            autoSave(entries, sessionPassword[0], frame);
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
            AutoClearCopy(selected.password, frame);
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
    protected static void refreshList(DefaultListModel<PasswordEntry> model, List<PasswordEntry> entries, String query) {
        model.clear();
        String q = query.toLowerCase();
        for (PasswordEntry elt : entries) {
            if (elt.label.toLowerCase().contains(q) || elt.username.toLowerCase().contains(q)){
               model.addElement(elt);
            }
        }
    }

    // Auto save method
    protected static void autoSave(List<PasswordEntry> entries, String masterPassword, JFrame frame) {
        try {
            SaveLogic.save(entries, masterPassword);
        } catch (Exception err) {
            JOptionPane.showMessageDialog(frame, "Data could not be saved.");
        }
    }

    // Auto-clear copy
    static void AutoClearCopy(String password, JFrame frame) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(password), null);

        javax.swing.Timer timer = new javax.swing.Timer(20000, ev -> {
            try {
                String current = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (current.equals(password)) {
                    clipboard.setContents(new StringSelection(""), null);
                }
            } catch (Exception ex) { }
        });
        timer.setRepeats(false);
        timer.start();

        JOptionPane.showMessageDialog(frame, "Password copied. Clipboard will be cleared in 20s if untouched.");
    }
}
