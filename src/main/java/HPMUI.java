import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class HPMUI {
    // Fields and buttons
    private final PWDGenOptions opt = new PWDGenOptions();
    private final List<PasswordEntry> entries = new ArrayList<>();
    private String sessionPassword = null;

    private final JFrame frame = new JFrame("HPM - Heimdall's password manager");
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JTextField searchField = new JTextField(24);
    private final DefaultListModel<PasswordEntry> listModel = new DefaultListModel<>();
    private final JList<PasswordEntry> entriesDisplay = new JList<>(listModel);
    private final JComboBox<String> sortedBox = new JComboBox<>(new String[]{
            "Label (A-Z)", "Label (Z-A)", "Username (A-Z)", "Username (Z-A)"
    });

    private final JButton unlockBtn   = new JButton("Unlock");
    private final JButton addEntry     = new JButton("Add");
    private final JButton editBtn      = new JButton("Edit");
    private final JButton deleteEntry  = new JButton("Delete");
    private final JButton seePWD       = new JButton("Show password");
    private final JButton copyPWD      = new JButton("Copy password");
    private final JButton TOTPbtn      = new JButton("2FA");
    private final JButton lockBtn      = new JButton("Lock");
    private final JButton changePwdBtn = new JButton("Change master password");

    private boolean autoLockEnabled = false;
    private int autoLockMinutes = 5;
    private javax.swing.Timer autoLockTimer;

    public static void main(String[] args) {
        com.formdev.flatlaf.FlatDarculaLaf.setup();
        SwingUtilities.invokeLater(HPMUI::new);
    }

    // UI constructor
    public HPMUI() {
        buildFrame();
        JPanel vaultPanel = buildVaultPanel();
        JPanel generatorPanel = buildGeneratorPanel();
        JPanel settingsPanel = buildSettingsPanel();
        buildNavigation(vaultPanel, generatorPanel, settingsPanel);
        wireHandlers();
        setupAutoLock();
        setUnlocked(false);
        frame.setVisible(true);
        passwordField.requestFocusInWindow();
        if (!vaultExists()) firstRunSetup();
    }

    // Initial build methods
    private void buildFrame() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
        ((JComponent) frame.getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    private void firstRunSetup() {
        JOptionPane.showMessageDialog(frame, "Welcome to HPM. Please setup a secure password that will protect" +
                " all your other passwords. It is highly advised not to store it online. Do not share it with anyone.");
        String pwd = null;
        while (pwd == null) pwd = promptNewPassword("Create master password");
        sessionPassword = pwd;
        entries.clear();
        autoSave();
        setUnlocked(true);
        JOptionPane.showMessageDialog(frame, "Your secure vault has been created, you're all set.");
    }

    private void setupAutoLock() {
        autoLockTimer = new Timer(autoLockMinutes * 60 * 1000, e -> onLock());
        autoLockTimer.setRepeats(false);

        Toolkit.getDefaultToolkit().addAWTEventListener(ev -> {
            if (autoLockTimer.isRunning()) autoLockTimer.restart();
        }, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    // Panel builders
    private JPanel buildVaultPanel() {
        JPanel topPanel = new JPanel();
        topPanel.add(passwordField);
        topPanel.add(unlockBtn);
        topPanel.add(lockBtn);

        JPanel searchSort = new JPanel(new BorderLayout());
        searchSort.add(searchField, BorderLayout.CENTER);
        searchSort.add(sortedBox, BorderLayout.EAST);

        JPanel listArea = new JPanel(new BorderLayout());
        listArea.add(searchSort, BorderLayout.NORTH);
        listArea.add(new JScrollPane(entriesDisplay), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addEntry);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteEntry);
        buttonPanel.add(seePWD);
        buttonPanel.add(copyPWD);
        buttonPanel.add(TOTPbtn);

        JPanel vaultPanel = new JPanel(new BorderLayout());
        vaultPanel.add(topPanel, BorderLayout.NORTH);
        vaultPanel.add(listArea, BorderLayout.CENTER);
        vaultPanel.add(buttonPanel, BorderLayout.SOUTH);
        return vaultPanel;
    }

    private JPanel buildGeneratorPanel() {
        JSpinner lengthSpinner  = new JSpinner(new SpinnerNumberModel(opt.length, 4, 64, 1));
        JCheckBox lowerBox      = new JCheckBox("Lowercase (a-z)", opt.useLower);
        JCheckBox upperBox      = new JCheckBox("Uppercase (A-Z)", opt.useUpper);
        JCheckBox numbersBox    = new JCheckBox("Digits (0-9)", opt.useNumbers);
        JCheckBox symbolBox     = new JCheckBox("Symbols (!@#...)", opt.useSymbols);
        JCheckBox ambiguousBox  = new JCheckBox("Exclude look-alikes", opt.excludeAmbiguous);
        JTextField genOutput    = new JTextField(16);
        genOutput.setEditable(false);
        JButton doGenerateBtn   = new JButton("Generate");
        JButton copyGenBtn      = new JButton("Copy");

        Runnable syncOptions = () -> {
            opt.length           = (int) lengthSpinner.getValue();
            opt.useLower         = lowerBox.isSelected();
            opt.useUpper         = upperBox.isSelected();
            opt.useNumbers       = numbersBox.isSelected();
            opt.useSymbols       = symbolBox.isSelected();
            opt.excludeAmbiguous = ambiguousBox.isSelected();
        };
        lowerBox.addActionListener(e -> syncOptions.run());
        upperBox.addActionListener(e -> syncOptions.run());
        numbersBox.addActionListener(e -> syncOptions.run());
        symbolBox.addActionListener(e -> syncOptions.run());
        ambiguousBox.addActionListener(e -> syncOptions.run());
        lengthSpinner.addChangeListener(e -> syncOptions.run());

        doGenerateBtn.addActionListener(e -> {
            syncOptions.run();
            genOutput.setText(PasswordGenerator.generatePassword(opt));
        });
        copyGenBtn.addActionListener(e -> autoClearCopy(genOutput.getText()));

        JPanel genContent = new JPanel(new GridLayout(0, 1, 5, 5));
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

        JPanel generatorPanel = new JPanel(new BorderLayout());
        generatorPanel.add(genContent, BorderLayout.NORTH);
        generatorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return generatorPanel;
    }

    private JPanel buildSettingsPanel() {
        JCheckBox autoLockBox = new JCheckBox("Enable auto-lock", autoLockEnabled);
        JSpinner autoLockTime = new JSpinner(new SpinnerNumberModel(autoLockMinutes, 1, 60, 1));
        autoLockTime.setEnabled(autoLockEnabled);

        autoLockBox.addActionListener(e -> {
            autoLockEnabled = autoLockBox.isSelected();
            autoLockTime.setEnabled(autoLockEnabled);
            if (autoLockEnabled && sessionPassword != null) autoLockTimer.restart();
            else autoLockTimer.stop();
        });

        autoLockTime.addChangeListener(e -> {
            autoLockMinutes = (int) autoLockTime.getValue();
            autoLockTimer.setInitialDelay(autoLockMinutes * 60 * 1000);
            autoLockTimer.setDelay(autoLockMinutes * 60 * 1000);
            if (autoLockTimer.isRunning()) autoLockTimer.restart();
        });

        JPanel content = new JPanel(new GridLayout(0, 1, 1, 5));
        content.add(new JLabel("Security"));
        content.add(changePwdBtn);
        content.add(autoLockBox);
        content.add(new JLabel("Auto-lock after (minutes):"));
        content.add(autoLockTime);

        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.add(content, BorderLayout.NORTH);
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        return settingsPanel;
    }

    private void buildNavigation(JPanel vaultPanel, JPanel generatorPanel, JPanel settingPanel) {
        CardLayout cardLayout = new CardLayout();
        JPanel contentArea = new JPanel(cardLayout);
        contentArea.add(vaultPanel, "Vault");
        contentArea.add(generatorPanel, "Generator");
        contentArea.add(settingPanel, "Settings");

        JButton navVault = new JButton("Vault");
        JButton navGen = new JButton("Generator");
        JButton navSettings = new JButton("Settings");
        navVault.addActionListener(e -> cardLayout.show(contentArea, "Vault"));
        navGen.addActionListener(e -> cardLayout.show(contentArea, "Generator"));
        navSettings.addActionListener(e -> cardLayout.show(contentArea, "Settings"));

        JPanel navButtons = new JPanel(new GridLayout(0, 1, 0, 8));
        navButtons.add(navVault);
        navButtons.add(navGen);
        navButtons.add(navSettings);
        JPanel navPanel = new JPanel(new BorderLayout());
        navPanel.add(navButtons, BorderLayout.NORTH);
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 15));

        frame.add(navPanel, BorderLayout.WEST);
        frame.add(contentArea, BorderLayout.CENTER);
    }

    // Update methods
    private void setUnlocked(boolean unlocked) {
        addEntry.setEnabled(unlocked);
        editBtn.setEnabled(unlocked);
        deleteEntry.setEnabled(unlocked);
        seePWD.setEnabled(unlocked);
        copyPWD.setEnabled(unlocked);
        TOTPbtn.setEnabled(unlocked);
        lockBtn.setEnabled(unlocked);
        changePwdBtn.setEnabled(unlocked);
        unlockBtn.setEnabled(!unlocked);
        if (unlocked && autoLockEnabled) autoLockTimer.restart();
        else autoLockTimer.stop();
    }

    private void refreshList() {
        listModel.clear();
        String q = searchField.getText().toLowerCase();
        for (PasswordEntry elt : entries) {
            if (elt.label.toLowerCase().contains(q) || elt.username.toLowerCase().contains(q))
                listModel.addElement(elt);
        }
    }

    private void autoSave() {
        try { SaveLogic.save(entries, sessionPassword); }
        catch (Exception err) { JOptionPane.showMessageDialog(frame, "Data could not be saved."); }
    }

    private String promptNewPassword(String title) {
        JPasswordField pwd1 = new JPasswordField(20);
        JPasswordField pwd2 = new JPasswordField(20);
        JPanel panel = new JPanel(new GridLayout(0, 1, 1, 5));
        panel.add(new JLabel("Master password:"));
        panel.add(pwd1);
        panel.add(new JLabel("Confirm password:"));
        panel.add(pwd2);

        int result = JOptionPane.showConfirmDialog(frame, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;

        String p1 = new String(pwd1.getPassword());
        String p2 = new String(pwd2.getPassword());
        if (p1.isBlank()) { JOptionPane.showMessageDialog(frame, "Password cannot be empty."); return null; }
        if (!p2.equals(p1)) { JOptionPane.showMessageDialog(frame, "Passwords must match."); return null; }
        return p1;
    }

    // Event-triggered methods
    private void wireHandlers() {
        passwordField.addActionListener(e -> unlockBtn.doClick());
        unlockBtn.addActionListener(e -> onUnlock());
        addEntry.addActionListener(e -> onAdd());
        editBtn.addActionListener(e -> onEdit());
        deleteEntry.addActionListener(e -> onDelete());
        seePWD.addActionListener(e -> onShowPassword());
        copyPWD.addActionListener(e -> onCopyPassword());
        TOTPbtn.addActionListener(e -> onTotp());
        lockBtn.addActionListener(e -> onLock());
        sortedBox.addActionListener(e -> onSort());
        changePwdBtn.addActionListener(e -> onChangePassword());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshList(); }
            public void removeUpdate(DocumentEvent e) { refreshList(); }
            public void changedUpdate(DocumentEvent e) { refreshList(); }
        });
        entriesDisplay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    PasswordEntry selected = entriesDisplay.getSelectedValue();
                    if (selected != null) autoClearCopy(selected.password);
                }
            }
        });
    }

    private void autoClearCopy(String password) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(password), null);
        Timer timer = new Timer(20000, ev -> {
            try {
                String current = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (current.equals(password)) clipboard.setContents(new StringSelection(""), null);
            } catch (Exception _) { }
        });
        timer.setRepeats(false);
        timer.start();
        JOptionPane.showMessageDialog(frame, "Password copied. Clipboard clears in 20s if untouched.");
    }

    private void onUnlock() {
        String masterPassword = new String(passwordField.getPassword());
        try {
            entries.clear();
            entries.addAll(LoadLogic.load(masterPassword));
            sessionPassword = masterPassword;
            refreshList();
            setUnlocked(true);
            passwordField.setText("");
            JOptionPane.showMessageDialog(frame, "Unlocked! " + entries.size() + " entries loaded.");
        } catch (Exception err) {
            JOptionPane.showMessageDialog(frame, "Wrong password!");
            passwordField.setText("");
        }
    }

    private void onAdd() {
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
        autoSave();
        refreshList();
    }

    private void onEdit() {
        PasswordEntry selected = entriesDisplay.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(frame, "No entry is selected.");
            return;
        }
        JTextField labelField = new JTextField(selected.label, 20);
        JTextField usernameField = new JTextField(selected.username, 20);
        JPasswordField passwordFieldEdit = new JPasswordField(selected.password, 20);
        JTextField totpField = new JTextField(selected.TOTPsecret == null ? "" : selected.TOTPsecret, 20);
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
        editPanel.add(new JLabel("2FA secret (optional):"));
        editPanel.add(totpField);

        // Show panel in one dialog with OK / cancel options
        int result = JOptionPane.showConfirmDialog(frame, editPanel, "Edit entry", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newLabel = labelField.getText();
        String newUsername = usernameField.getText();
        String newPassword = new String(passwordFieldEdit.getPassword());
        String newTotp = totpField.getText().trim();
        selected.TOTPsecret = newTotp.isBlank() ? null : newTotp;

        if (newLabel.isBlank() || newUsername.isBlank() || newPassword.isBlank()) {
            JOptionPane.showMessageDialog(frame, "All fields are required.");
            return;
        }

        selected.label = newLabel;
        selected.username = newUsername;
        selected.password = newPassword;

        autoSave();
        refreshList();
    }

    private void onDelete() {
        PasswordEntry selected = entriesDisplay.getSelectedValue();
        if (selected == null) { JOptionPane.showMessageDialog(frame, "No entry is selected"); return; }
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Do you really want to delete \"" + selected + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        entries.remove(selected);
        refreshList();
        autoSave();
    }

    private void onShowPassword() {
        PasswordEntry selected = entriesDisplay.getSelectedValue();
        if (selected == null) { JOptionPane.showMessageDialog(frame, "No entry is selected."); return; }
        JOptionPane.showMessageDialog(frame, "The password is: " + selected.password);
    }

    private void onCopyPassword() {
        PasswordEntry selected = entriesDisplay.getSelectedValue();
        if (selected == null) { JOptionPane.showMessageDialog(frame, "No entry is selected."); return; }
        autoClearCopy(selected.password);
    }

    private void onTotp() {
        PasswordEntry selected = entriesDisplay.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(frame, "No entry is selected.");
            return;
        }

        // If there is no secret yet, set one up
        if (selected.TOTPsecret == null || selected.TOTPsecret.isBlank()) {
            String secret = JOptionPane.showInputDialog(frame, "Enter the 2FA secret (Base 32):");
            if (secret == null || secret.isBlank()) return;
            selected.TOTPsecret = secret.trim();
            autoSave();
            JOptionPane.showMessageDialog(frame, "2FA secret saved.");
            return;
        }

        // If a secret has been set, show the 2FA code
        try {
            String code = TOTP.generateCode(selected.TOTPsecret);
            long secondsLeft = 30 - (System.currentTimeMillis() / 1000L % 30);
            JOptionPane.showMessageDialog(frame,
                    "Code: " + code + "\nExpires in " + secondsLeft + "s");
        } catch (Exception err) {
            JOptionPane.showMessageDialog(frame, "Invalid 2FA secret.");
        }
    }

    private void onLock() {
        entries.clear();
        listModel.clear();
        passwordField.setText("");
        sessionPassword = null;
        setUnlocked(false);
    }

    private void onSort() {
        String choice = (String) sortedBox.getSelectedItem();
        switch (choice) {
            case "Label (A-Z)"    -> entries.sort(java.util.Comparator.comparing(x -> x.label.toLowerCase()));
            case "Label (Z-A)"    -> entries.sort(java.util.Comparator.comparing((PasswordEntry x) -> x.label.toLowerCase()).reversed());
            case "Username (A-Z)" -> entries.sort(java.util.Comparator.comparing(x -> x.username.toLowerCase()));
            case "Username (Z-A)" -> entries.sort(java.util.Comparator.comparing((PasswordEntry x) -> x.username.toLowerCase()).reversed());
        }
        refreshList();
    }

    private void onChangePassword() {
        String pwd = promptNewPassword("Change master password");
        if (pwd == null) return;
        sessionPassword = pwd;
        autoSave();
        JOptionPane.showMessageDialog(frame, "Master password successfully changed.");
    }

    // Utility
    private boolean vaultExists() {
        try {
            java.nio.file.Path file = java.nio.file.Path.of("vault.dat");
            return java.nio.file.Files.exists(file) && java.nio.file.Files.size(file) > 0;
        } catch (Exception err) {
            return false;
        }
    }
}
