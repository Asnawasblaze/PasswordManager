package com.passwordmanager.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.UIManager;

import com.passwordmanager.service.AuthService;
import com.passwordmanager.util.PasswordStrengthChecker;
import com.passwordmanager.util.TotpUtil.TotpSetupInfo;

public class RegistrationDialog extends JDialog {

    private final AuthService authService;

    // Form fields
    private JTextField usernameField;
    private JPasswordField masterPasswordField;
    private JPasswordField confirmPasswordField;

    // UI helpers
    private JButton registerButton;
    private JLabel matchLabel;
    private JProgressBar strengthBar;
    private JLabel strengthText;

    public RegistrationDialog(JDialog parent, AuthService authService) {
        super(parent, "Create Account", true);
        this.authService = authService;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(parent);
        getRootPane().setDefaultButton(registerButton);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setContentPane(root);

        // Left gradient branding
        JPanel left = new GradientPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(560, 0));
        left.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 40));

        JLabel t1 = new JLabel("Register as");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 44));
        t1.setForeground(new Color(41, 50, 65));
        JLabel t2 = new JLabel("New User");
        t2.setFont(new Font("Segoe UI", Font.BOLD, 52));
        t2.setForeground(new Color(26, 172, 120));
        JLabel tagline = new JLabel("Create your secure account");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        tagline.setForeground(new Color(70, 82, 96));
        JLabel sub = new JLabel("Join thousands of users protecting their passwords");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(120, 130, 140));

        t1.setAlignmentX(Component.LEFT_ALIGNMENT);
        t2.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(Box.createVerticalGlue());
        left.add(t1);
        left.add(t2);
        left.add(Box.createVerticalStrut(16));
        left.add(tagline);
        left.add(Box.createVerticalStrut(8));
        left.add(sub);
        left.add(Box.createVerticalGlue());

        // Right card with form
        RoundedPanel card = new RoundedPanel(24, UIManager.getColor("Panel.background"));
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        card.add(title, gbc);

        gbc.gridy++;
        JLabel subtitle = new JLabel("Set up your new password manager");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(95, 98, 109));
        card.add(subtitle, gbc);

        // Fields
        gbc.gridwidth = 1;
        gbc.gridy++; gbc.gridx = 0;
        card.add(new JLabel("Full Name"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(18);
        usernameField.setToolTipText("Enter your full name or preferred username.");
        card.add(usernameField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        card.add(new JLabel("Master Password"), gbc);
        gbc.gridx = 1;
        masterPasswordField = new JPasswordField(18);
        masterPasswordField.setToolTipText("Your primary, unrecoverable key. Used to decrypt all saved passwords.");
        card.add(masterPasswordField, gbc);

        // Strength meter row (progress bar + label)
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        JPanel strengthRow = new JPanel(new BorderLayout(8, 0));
        strengthBar = new JProgressBar(0, 100);
        strengthBar.setStringPainted(false);
        strengthBar.setForeground(new Color(40, 180, 90));
        strengthText = new JLabel("");
        strengthText.setForeground(new Color(30, 150, 70));
        strengthRow.add(strengthBar, BorderLayout.CENTER);
        strengthRow.add(strengthText, BorderLayout.EAST);
        card.add(strengthRow, gbc);

        // Confirm Password
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1;
        card.add(new JLabel("Confirm Password"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(18);
        confirmPasswordField.setToolTipText("Retype the master password to confirm.");
        card.add(confirmPasswordField, gbc);

        // Match label
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        matchLabel = new JLabel(" ");
        card.add(matchLabel, gbc);

        // Buttons
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        registerButton = new JButton("Register and Setup 2FA");
        registerButton.addActionListener(e -> attemptRegistration());
        card.add(registerButton, gbc);

        gbc.gridy++;
        JButton backBtn = new JButton("Back to Login");
        backBtn.addActionListener(e -> dispose());
        card.add(backBtn, gbc);

        // Combine
        JPanel rightWrapper = new JPanel(new GridBagLayout());
        GridBagConstraints r = new GridBagConstraints();
        r.gridx = 0; r.gridy = 0; r.weightx = 1; r.weighty = 1; r.fill = GridBagConstraints.BOTH;
        rightWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightWrapper.add(card, r);

        root.add(left, BorderLayout.CENTER);
        root.add(rightWrapper, BorderLayout.EAST);

        // Live updates
        masterPasswordField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateStrength());
        confirmPasswordField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateMatch());
        masterPasswordField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateMatch());

        // Initial state
        updateStrength();
        updateMatch();
    }

    private void updateStrength() {
        String pwd = new String(masterPasswordField.getPassword());
        String strength = PasswordStrengthChecker.checkStrength(pwd);
        int val; Color col; String label;
        switch (strength) {
            case "Strong":
                val = 100; col = new Color(40, 180, 90); label = "Strong"; break;
            case "Moderate":
                val = 66; col = new Color(255, 170, 50); label = "Moderate"; break;
            default:
                val = 33; col = new Color(210, 60, 60); label = "Weak"; break;
        }
        strengthBar.setValue(val);
        strengthBar.setForeground(col);
        strengthText.setText(label);
    }

    private void updateMatch() {
        String a = new String(masterPasswordField.getPassword());
        String b = new String(confirmPasswordField.getPassword());
        if (a.isEmpty() && b.isEmpty()) {
            matchLabel.setText(" ");
            return;
        }
        if (a.equals(b)) {
            matchLabel.setForeground(new Color(40, 180, 90));
            matchLabel.setText("Passwords match");
        } else {
            matchLabel.setForeground(new Color(210, 60, 60));
            matchLabel.setText("Passwords do not match");
        }
    }

    private void attemptRegistration() {
        String username = usernameField.getText().trim();
        String password = new String(masterPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // Basic Validation
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Optional: nudge for stronger passwords (do not block minimal change)
        String strength = PasswordStrengthChecker.checkStrength(password);
        if ("Weak".equals(strength)) {
            int res = JOptionPane.showConfirmDialog(this, "Your master password looks weak. Continue anyway?", "Low Strength", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res != JOptionPane.YES_OPTION) return;
        }

        // Disable button to avoid double submits
        registerButton.setEnabled(false);
        registerButton.setText("Registering...");

        // Call Service
        TotpSetupInfo totpInfo = authService.registerUser(username, password);

        registerButton.setEnabled(true);
        registerButton.setText("Register and Setup 2FA");

        if (totpInfo != null) {
            try {
                TotpSetupDialog setup = new TotpSetupDialog(this, totpInfo.getSecret(), totpInfo.getQrCodeUri());
                setup.setVisible(true);
            } catch (Exception ignore) {
                showTotpSetup(totpInfo.getQrCodeUri());
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Registration failed. Username may already exist.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTotpSetup(String qrCodeUri) {
        JEditorPane instructions = new JEditorPane("text/html",
                "<html><body style='font-family: Segoe UI, Arial; padding: 10px;'>" +
                        "<h2>2FA Setup Required</h2>" +
                        "<p>Registration successful! Link your account to an authenticator app now.</p>" +
                        "<ol>" +
                        "<li>Open your authenticator app on your phone.</li>" +
                        "<li>Scan the QR code displayed when you click the link below.</li>" +
                        "<li>Enter a 6-digit code at login.</li>" +
                        "</ol>" +
                        "<p><a href='" + qrCodeUri + "'>Click here to view the QR code</a></p>" +
                        "</body></html>");
        instructions.setEditable(false);
        instructions.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Could not open browser. Manually use this URI:\n" + e.getURL().toString(), "URI Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JOptionPane.showMessageDialog(this, instructions, "2FA Setup Required", JOptionPane.INFORMATION_MESSAGE);
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
            // Soft mint gradient similar to the mock
            Color c1 = new Color(235, 250, 245);
            Color c2 = new Color(210, 245, 230);
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