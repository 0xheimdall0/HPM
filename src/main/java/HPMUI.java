import com.password4j.Password;

import javax.swing.*;

public class HPMUI {
    public static void main(String[] args) {
        // Initiate the frame
        JFrame frame = new JFrame("HPM - Heimdall's password manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1920, 1080);
        frame.setLayout(new java.awt.FlowLayout());

        // Add the first buttons
        JButton generatePWD = new JButton("Generate Password");
        JTextField output = new JTextField(37);

        // Add the items to the frame
        frame.add(generatePWD);
        frame.add(output);

        // Run the code when the button is clicked
        generatePWD.addActionListener(e -> {
            String PWD = PasswordGenerator.generatePassword(16);
            output.setText(PWD);
        });

        frame.setVisible(true);
    }
}
