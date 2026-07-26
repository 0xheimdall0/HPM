import com.formdev.flatlaf.FlatDarculaLaf;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
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
    private Timer autoLockTimer;

    public static void main(String[] args) {
        FlatDarculaLaf.setup();
        SwingUtilities.invokeLater(HPMUI::new);
    }

    // UI constructor
    public HPMUI() {
        loadSettings();
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
        autoLockTimer = new Timer(autoLockMinutes * 60 * 1000, _ -> onLock());
        autoLockTimer.setRepeats(false);

        Toolkit.getDefaultToolkit().addAWTEventListener(_ -> {
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
        lowerBox.addActionListener(_ -> syncOptions.run());
        upperBox.addActionListener(_ -> syncOptions.run());
        numbersBox.addActionListener(_ -> syncOptions.run());
        symbolBox.addActionListener(_ -> syncOptions.run());
        ambiguousBox.addActionListener(_ -> syncOptions.run());
        lengthSpinner.addChangeListener(_ -> syncOptions.run());

        doGenerateBtn.addActionListener(_ -> {
            syncOptions.run();
            genOutput.setText(PasswordGenerator.generatePassword(opt));
        });
        copyGenBtn.addActionListener(_ -> autoClearCopy(genOutput.getText()));

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

        autoLockBox.addActionListener(_ -> {
            autoLockEnabled = autoLockBox.isSelected();
            autoLockTime.setEnabled(autoLockEnabled);
            if (autoLockEnabled && sessionPassword != null) autoLockTimer.restart();
            else autoLockTimer.stop();
            saveSettings();
        });

        autoLockTime.addChangeListener(_ -> {
            autoLockMinutes = (int) autoLockTime.getValue();
            autoLockTimer.setInitialDelay(autoLockMinutes * 60 * 1000);
            autoLockTimer.setDelay(autoLockMinutes * 60 * 1000);
            if (autoLockTimer.isRunning()) autoLockTimer.restart();
            saveSettings();
        });

        JButton securityCheckBtn = new JButton("Security check");
        securityCheckBtn.addActionListener(_ -> onSecurityCheck());

        JPanel content = new JPanel(new GridLayout(0, 1, 1, 5));
        content.add(new JLabel("Security"));
        content.add(changePwdBtn);
        content.add(autoLockBox);
        content.add(new JLabel("Auto-lock after (minutes):"));
        content.add(autoLockTime);
        content.add(securityCheckBtn);

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
        navVault.addActionListener(_ -> cardLayout.show(contentArea, "Vault"));
        navGen.addActionListener(_ -> cardLayout.show(contentArea, "Generator"));
        navSettings.addActionListener(_ -> cardLayout.show(contentArea, "Settings"));

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
        passwordField.addActionListener( _ -> unlockBtn.doClick());
        unlockBtn.addActionListener(_ -> onUnlock());
        addEntry.addActionListener(_ -> onAdd());
        editBtn.addActionListener(_ -> onEdit());
        deleteEntry.addActionListener(_ -> onDelete());
        seePWD.addActionListener(_ -> onShowPassword());
        copyPWD.addActionListener(_ -> onCopyPassword());
        TOTPbtn.addActionListener(_ -> onTotp());
        lockBtn.addActionListener(_ -> onLock());
        sortedBox.addActionListener(_ -> onSort());
        changePwdBtn.addActionListener(_ -> onChangePassword());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshList(); }
            public void removeUpdate(DocumentEvent e) { refreshList(); }
            public void changedUpdate(DocumentEvent e) { refreshList(); }
        });
        entriesDisplay.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
        Timer timer = new Timer(20000, _ -> {
            try {
                String current = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (current.equals(password)) clipboard.setContents(new StringSelection(""), null);
            } catch (Exception _) { }
        });
        timer.setRepeats(false);
        timer.start();
        JOptionPane.showMessageDialog(frame, "Copied. Clipboard clears in 20s if untouched.");
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
        }
        String username = JOptionPane.showInputDialog("Username: ");
        if (username == null) return;
        else if (username.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Every field is required.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(frame, "Generate password randomly?",
                "Password", JOptionPane.YES_NO_OPTION);
        String password;
        if (choice == JOptionPane.YES_OPTION) {
            password = PasswordGenerator.generatePassword(opt);
        } else {
            JPasswordField pwField = new JPasswordField(20);
            int res = JOptionPane.showConfirmDialog(frame, pwField, "Enter password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            password = new String(pwField.getPassword());
            if (password.isBlank()) {
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
        showPWD.addActionListener(_ -> {
            if (showPWD.isSelected()) passwordFieldEdit.setEchoChar((char) 0);
            else passwordFieldEdit.setEchoChar(maskChar);
        });

        // Stack every item vertically in the edit panel
        JPanel editPanel = new JPanel(new GridLayout(0, 1, 5, 5));
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
        showLiveTotp(selected);
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
            case "Label (A-Z)"    -> entries.sort(Comparator.comparing(x -> x.label.toLowerCase()));
            case "Label (Z-A)"    -> entries.sort(Comparator.comparing((PasswordEntry x) -> x.label.toLowerCase()).reversed());
            case "Username (A-Z)" -> entries.sort(Comparator.comparing(x -> x.username.toLowerCase()));
            case "Username (Z-A)" -> entries.sort(Comparator.comparing((PasswordEntry x) -> x.username.toLowerCase()).reversed());
            case null -> {}
            default -> throw new IllegalStateException("Unexpected value: " + choice);
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

    private void onSecurityCheck() {
        if (sessionPassword == null) {
            JOptionPane.showMessageDialog(frame, "Please unlock the vault first.");
            return;
        }

        Map<String, Integer> counts = new HashMap<>();
        for (PasswordEntry elt : entries) {
            counts.merge(elt.password, 1, Integer::sum);
        }

        // Build problem report
        StringBuilder report = new StringBuilder();
        for (PasswordEntry elt : entries) {
            List<String> issues = new ArrayList<>();
            int c = counts.get(elt.password);
            if (elt.password.length() < 12) issues.add("too short");
            if (charClasses(elt.password) < 3) issues.add("low variety");
            if (c > 1) issues.add("reused by " + c + " entries");
            if (!issues.isEmpty()) {
                report.append(elt.label).append(" - ").append(String.join(", ", issues)).append("\n");
            }
        }

        String message = report.isEmpty() ? "No issues found." : report.toString();
        JTextArea area = new JTextArea(message, 12, 48);
        area.setEditable(false);
        JOptionPane.showMessageDialog(frame, new JScrollPane(area), "Security check", JOptionPane.INFORMATION_MESSAGE);
    }

    // Utility
    private boolean vaultExists() {
        try {
            Path file = Path.of("vault.dat");
            return Files.exists(file) && Files.size(file) > 0;
        } catch (Exception err) {
            return false;
        }
    }

    private void showLiveTotp(PasswordEntry entry) {
        JDialog dialog = new JDialog(frame, "2FA code", false);
        JLabel codeLabel = new JLabel("", SwingConstants.CENTER);
        codeLabel.setFont(codeLabel.getFont().deriveFont(28f));
        JLabel countDownLabel = new JLabel("", SwingConstants.CENTER);
        JButton copyBtn = new JButton("Copy");

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panel.add(countDownLabel, BorderLayout.NORTH);
        panel.add(codeLabel, BorderLayout.CENTER);
        panel.add(copyBtn, BorderLayout.SOUTH);

        Runnable update = () -> {
            try {
                String code = TOTP.generateCode(entry.TOTPsecret);
                long secondsLeft = 30 - (System.currentTimeMillis() / 1000L % 30);
                codeLabel.setText(code);
                countDownLabel.setText("Expires in " + secondsLeft + "s");
            } catch (Exception err) {
                codeLabel.setText("Invalid secret.");
                countDownLabel.setText("");
            }
        };
        update.run();
        Timer timer = new Timer(1000, _ -> update.run());
        timer.start();

        copyBtn.addActionListener(_ -> {
            try { autoClearCopy(TOTP.generateCode(entry.TOTPsecret)); } catch (Exception _ ) { }
        });

        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { timer.stop(); }
        });

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private int charClasses(String pw) {
        int classes = 0;
        if (pw.matches(".*[a-z].*")) classes++;   // has lowercase
        if (pw.matches(".*[A-Z].*")) classes++;   // has uppercase
        if (pw.matches(".*[0-9].*")) classes++;   // has a digit
        if (pw.matches(".*[^a-zA-Z0-9].*")) classes++;  // has a symbol
        return classes;
    }

    // Persistence handlers
    private void loadSettings() {
        Path file = Path.of("settings.properties");
        if (!Files.exists(file)) return;
        Properties props = new Properties();
        try (var in = Files.newInputStream(file)) {
            props.load(in);
            autoLockEnabled = Boolean.parseBoolean(props.getProperty("autoLockEnabled", "false"));
            autoLockMinutes = Integer.parseInt(props.getProperty("autoLockMinutes", "5"));
        } catch (Exception _) { }
    }

    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("autoLockEnabled", String.valueOf(autoLockEnabled));
        props.setProperty("autoLockMinutes", String.valueOf(autoLockMinutes));
        try (var out = Files.newOutputStream(Path.of("settings.properties"))) {
            props.store(out, "HPM settings");
        } catch (Exception _) { }
    }
}
