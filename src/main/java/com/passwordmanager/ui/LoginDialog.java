package com.passwordmanager.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;

import com.passwordmanager.model.User;
import com.passwordmanager.service.AuthService;

public class LoginDialog extends JDialog {

    private final AuthService authService = new AuthService();

    // UI Components
    private JTextField usernameField;
    private JPasswordField masterPasswordField;
    private JTextField totpField;
    private JButton loginButton;

    public LoginDialog() {
        setTitle("Password Manager - Login");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null); // Center the dialog
        getRootPane().setDefaultButton(loginButton);
    }

    private void initUI() {
        // Root container with padding
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setContentPane(root);

        // Left branding panel (gradient)
        JPanel left = new GradientPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(560, 0));
        left.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 40));

        JLabel brand1 = new JLabel("Password");
        brand1.setFont(new Font("Segoe UI", Font.BOLD, 56));
        brand1.setForeground(new Color(41, 50, 65));
        JLabel brand2 = new JLabel("Manager");
        brand2.setFont(new Font("Segoe UI", Font.BOLD, 56));
        brand2.setForeground(new Color(86, 97, 255));

        JLabel tagline = new JLabel("Secure • Simple • Reliable");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        tagline.setForeground(new Color(70, 82, 96));

        brand1.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand2.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(Box.createVerticalGlue());
        left.add(brand1);
        left.add(brand2);
        left.add(Box.createVerticalStrut(16));
        left.add(tagline);
        left.add(Box.createVerticalGlue());

        // Right card panel
        RoundedPanel card = new RoundedPanel(24, UIManager.getColor("Panel.background"));
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(welcome, gbc);

        gbc.gridy++;
        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(95, 98, 109));
        card.add(subtitle, gbc);

        // Fields
        gbc.gridwidth = 1;
        gbc.gridy++; gbc.gridx = 0;
        card.add(new JLabel("Username"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(18);
        usernameField.setToolTipText("Enter your username");
        card.add(usernameField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        card.add(new JLabel("Master Password"), gbc);
        gbc.gridx = 1;
        masterPasswordField = new JPasswordField(18);
        masterPasswordField.setToolTipText("Your primary, unrecoverable key. Used to decrypt all saved passwords.");
        card.add(masterPasswordField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        card.add(new JLabel("TOTP Code"), gbc);
        gbc.gridx = 1;
        totpField = new JTextField(18);
        totpField.setToolTipText("Time-based One-Time Password. Get this 6-digit code from the authenticator app you set up during registration.");
        card.add(totpField, gbc);

        // Buttons
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        loginButton = new JButton("Login Securely");
        loginButton.addActionListener(e -> attemptLogin());
        card.add(loginButton, gbc);

        gbc.gridy++;
        JButton registerButton = new JButton("Register New User");
        registerButton.addActionListener(e -> showRegistrationDialog());
        card.add(registerButton, gbc);

        gbc.gridy++;
        JLabel help = new JLabel("New user? Click \"Register New User\" to get started");
        help.setForeground(new Color(95, 98, 109));
        card.add(help, gbc);

        // Combine left and right
        JPanel rightWrapper = new JPanel(new GridBagLayout());
        GridBagConstraints r = new GridBagConstraints();
        r.gridx = 0; r.gridy = 0; r.weightx = 1; r.weighty = 1; r.fill = GridBagConstraints.BOTH;
        rightWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightWrapper.add(card, r);

        root.add(left, BorderLayout.CENTER);
        root.add(rightWrapper, BorderLayout.EAST);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(masterPasswordField.getPassword());
        String totp = totpField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || totp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Login Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!totp.matches("\\d{6}")) {
            JOptionPane.showMessageDialog(this, "TOTP code must be a 6-digit number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Step 1: Authenticate Master Password
        Optional<User> userOpt = authService.authenticateMasterPassword(username, password);
        if (userOpt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid Username or Master Password.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Derive the session master key (32 bytes) once
        byte[] masterKeyBytes;
        try {
            String storedSalt = userOpt.get().getMasterSalt();
            masterKeyBytes = com.passwordmanager.util.Pbkdf2HashUtil.hashPassword(
                    password, com.passwordmanager.util.Pbkdf2HashUtil.fromBase64(storedSalt));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Security Key Derivation Failed.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Step 2: Verify TOTP inline
        int code = Integer.parseInt(totp);
        boolean ok = authService.authenticateTotp(userOpt.get(), code, masterKeyBytes);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Invalid TOTP Code.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Success → open MainFrame
        JOptionPane.showMessageDialog(this, "Login Successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
        dispose();
        MainFrame main = new MainFrame(userOpt.get(), masterKeyBytes);
        main.setVisible(true);
    }

    private void showRegistrationDialog() {
        RegistrationDialog registrationDialog = new RegistrationDialog(this, authService);
        registrationDialog.setVisible(true);
    }

    // ----- Helper Panels -----
    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            // Soft lavender to very light blue, similar to the mock
            Color c1 = new Color(242, 246, 255);
            Color c2 = new Color(225, 232, 250);
            g2.setPaint(new GradientPaint(0, 0, c1, w, h, c2));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color bg;
        RoundedPanel(int arc, Color bg) {
            this.arc = arc;
            this.bg = bg == null ? Color.WHITE : bg;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}