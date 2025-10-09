package com.passwordmanager.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import com.passwordmanager.service.PasswordService;
import com.passwordmanager.util.PasswordStrengthChecker;

public class AddPasswordDialog extends JDialog {

    private final MainFrame parentFrame;
    private final User loggedInUser;
    private final byte[] masterKeyBytes; // 32-byte key derived once at login
    private final PasswordService passwordService = new PasswordService();

    // UI Components
    private JTextField titleField, usernameField, passwordField, strengthMeter, noteField;
    private JButton generateButton;
    private JButton saveButton;

    public AddPasswordDialog(MainFrame parent, User user, byte[] masterKeyBytes) {
        super(parent, "Add/Edit Password Entry", true);
        this.parentFrame = parent;
        this.loggedInUser = user;
        this.masterKeyBytes = masterKeyBytes;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        // Use a simple GridBagLayout for flexible form layout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Row 1: Title ---
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; mainPanel.add(new JLabel("Title (e.g., Google):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; titleField = new JTextField(25); mainPanel.add(titleField, gbc);

        // --- Row 2: Username ---
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1; mainPanel.add(new JLabel("Username/Email:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; usernameField = new JTextField(25); mainPanel.add(usernameField, gbc);

        // --- Row 3: Password Field ---
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1; mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; passwordField = new JTextField(15); passwordField.putClientProperty("JComponent.roundRect", true); mainPanel.add(passwordField, gbc);
        
        // Add password field listener for strength check
        passwordField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateStrengthMeter());

        // --- Row 3: Generate Button ---
        gbc.gridx = 2; gbc.gridwidth = 1; generateButton = new JButton("Generate"); mainPanel.add(generateButton, gbc);
        generateButton.addActionListener(e -> showGeneratorDialog());

        // --- Row 4: Strength Meter ---
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1; mainPanel.add(new JLabel("Strength:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; strengthMeter = new JTextField("Weak"); strengthMeter.setEditable(false); mainPanel.add(strengthMeter, gbc);
        
        // --- Row 5: Note ---
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1; mainPanel.add(new JLabel("Notes:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; noteField = new JTextField(25); mainPanel.add(noteField, gbc);
        
        // --- Row 6: Save Button ---
        gbc.gridy++; gbc.gridx = 1; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.EAST; 
        saveButton = new JButton("Save Entry");
        saveButton.setPreferredSize(new Dimension(150, 30));
        saveButton.addActionListener(e -> saveEntry());
        mainPanel.add(saveButton, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void updateStrengthMeter() {
        String password = passwordField.getText();
        String strength = PasswordStrengthChecker.checkStrength(password);
        strengthMeter.setText(strength);
        
        // Color coding for premium look
        Color color;
        switch (strength) {
            case "Strong": color = new Color(50, 200, 50); break; // Green
            case "Moderate": color = new Color(255, 180, 50); break; // Orange
            case "Weak": 
            default: color = new Color(200, 50, 50); break; // Red
        }
        strengthMeter.setForeground(color);
    }

    private void showGeneratorDialog() {
    PasswordGeneratorDialog generatorDialog = new PasswordGeneratorDialog(parentFrame);
    generatorDialog.setVisible(true);
    
    // Retrieve password after dialog closes
    String generated = generatorDialog.getGeneratedPassword();
    
    if (generated != null) {
        passwordField.setText(generated);
        updateStrengthMeter(); // Update strength for the new password
    }
}
    
    private void saveEntry() {
        // Basic validation
        if (titleField.getText().trim().isEmpty() || passwordField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and Password are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean success = passwordService.createEntry(
            loggedInUser.getId(),
            titleField.getText().trim(),
            usernameField.getText().trim(),
            passwordField.getText(),
            noteField.getText().trim(),
            masterKeyBytes
        );

        if (success) {
            JOptionPane.showMessageDialog(this, "Password entry saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            parentFrame.loadPasswordData(); // Refresh the main table
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to save entry. Check logs.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
// Helper functional interface for DocumentListener simplicity
interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
    void update(javax.swing.event.DocumentEvent e);
    @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
}