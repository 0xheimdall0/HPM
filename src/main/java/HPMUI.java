import com.password4j.Password;

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

        // Fields
        JTextField outputPWD = new JTextField(37);
        JPasswordField passwordField = new JPasswordField(36);

        // Output fields
        JTextArea entriesDisplay = new JTextArea(10, 30);
        entriesDisplay.setEditable(false);

        // Add the items to the frame
        frame.add(generatePWD);
        frame.add(outputPWD);
        frame.add(passwordField);
        frame.add(unlockBtn);
        frame.add(new JScrollPane(entriesDisplay));

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
                refreshList(entriesDisplay, entries);
                JOptionPane.showMessageDialog(frame, "Unlocked successfully! " + entries.size() + " entries loaded.");
            } catch (Exception err) {
                JOptionPane.showMessageDialog(frame, "Wrong password!");
            }
        });

        frame.setVisible(true);
    }

    protected static void refreshList(JTextArea area, List<PasswordEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (PasswordEntry elt : entries) {
            sb.append(elt.label).append(" - ").append(elt.username).append("\n");
        }
        area.setText(sb.toString());
    }
}
