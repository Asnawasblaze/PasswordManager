package com.passwordmanager.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.passwordmanager.model.User;
import com.passwordmanager.service.AuthService;

public class TotpVerificationDialog extends JDialog {

    private final AuthService authService;
    private final User user;
    private final byte[] masterKeyBytes;

    private JTextField codeField;

    public TotpVerificationDialog(JDialog parent, AuthService authService, User user, byte[] masterKeyBytes) {
        super(parent, "2 Factor Authentication", true);
        this.authService = authService;
        this.user = user;
        this.masterKeyBytes = masterKeyBytes;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel title = new JLabel("2 Factor Authentication");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        titlePanel.add(title);
        add(titlePanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Verification Code"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        codeField = new JTextField(10);
        codeField.setToolTipText("Enter the 6-digit code from your authenticator app");
        formPanel.add(codeField, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backBtn = new JButton("Back to Login");
        JButton verifyBtn = new JButton("Verify");
        actions.add(backBtn);
        actions.add(verifyBtn);
        add(actions, BorderLayout.SOUTH);

        backBtn.addActionListener(e -> dispose());
        verifyBtn.addActionListener(e -> verify());
        getRootPane().setDefaultButton(verifyBtn);
    }

    private void verify() {
        String codeStr = codeField.getText().trim();
        if (codeStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the 6-digit code.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int code = Integer.parseInt(codeStr);
            if (authService.authenticateTotp(user, code, masterKeyBytes)) {
                JOptionPane.showMessageDialog(this, "Login Successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                // Close both dialogs and show main app
                JDialog parent = (JDialog) getParent();
                dispose();
                if (parent != null) parent.dispose();
                MainFrame main = new MainFrame(user, masterKeyBytes);
                main.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid TOTP Code.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "TOTP Code must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
