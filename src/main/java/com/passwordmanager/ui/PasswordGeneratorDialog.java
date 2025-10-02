package com.passwordmanager.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import com.passwordmanager.util.PasswordGenerator;

public class PasswordGeneratorDialog extends JDialog {

    private String generatedPassword;
    private JSlider lengthSlider;
    private JCheckBox upperCase, lowerCase, digits, symbols;
    private JLabel passwordLabel;
    
    // Default settings
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 32;
    private static final int DEFAULT_LENGTH = 16;

    public PasswordGeneratorDialog(JFrame parent) {
        super(parent, "Secure Password Generator", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        initUI();
        generateAndUpdate(); // Generate initial password
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        
        // --- Output Panel ---
        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        passwordLabel = new JLabel("Click Generate");
        passwordLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        passwordLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        outputPanel.add(passwordLabel);
        add(outputPanel, BorderLayout.NORTH);

        // --- Controls Panel ---
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Length Slider
        gbc.gridx = 0; gbc.gridy = 0; controlsPanel.add(new JLabel("Length:"), gbc);
        lengthSlider = new JSlider(MIN_LENGTH, MAX_LENGTH, DEFAULT_LENGTH);
        lengthSlider.setMajorTickSpacing(8);
        lengthSlider.setMinorTickSpacing(1);
        lengthSlider.setPaintTicks(true);
        lengthSlider.setPaintLabels(true);
        lengthSlider.addChangeListener(e -> generateAndUpdate());
        gbc.gridx = 1; controlsPanel.add(lengthSlider, gbc);

        // 2. Character Sets (Checkboxes)
        ChangeListener checkboxListener = e -> generateAndUpdate();
        
        gbc.gridy++; gbc.gridx = 0; upperCase = new JCheckBox("A-Z (Uppercase)", true); upperCase.addChangeListener(checkboxListener); controlsPanel.add(upperCase, gbc);
        gbc.gridx = 1; lowerCase = new JCheckBox("a-z (Lowercase)", true); lowerCase.addChangeListener(checkboxListener); controlsPanel.add(lowerCase, gbc);
        
        gbc.gridy++; gbc.gridx = 0; digits = new JCheckBox("0-9 (Digits)", true); digits.addChangeListener(checkboxListener); controlsPanel.add(digits, gbc);
        gbc.gridx = 1; symbols = new JCheckBox("!@#$% (Symbols)", true); symbols.addChangeListener(checkboxListener); controlsPanel.add(symbols, gbc);
        
        add(controlsPanel, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton("Copy & Close");
        JButton regenerateButton = new JButton("Regenerate");
        
        copyButton.addActionListener(e -> {
            // Note: In Phase 7, implement auto-clear clipboard security feature
            generatedPassword = passwordLabel.getText();
            dispose();
        });
        
        regenerateButton.addActionListener(e -> generateAndUpdate());
        
        buttonPanel.add(regenerateButton);
        buttonPanel.add(copyButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void generateAndUpdate() {
        int length = lengthSlider.getValue();
        
        // Basic check to prevent errors
        if (!upperCase.isSelected() && !lowerCase.isSelected() && !digits.isSelected() && !symbols.isSelected()) {
            passwordLabel.setText("Select at least one character set.");
            return;
        }

        try {
            String newPassword = PasswordGenerator.generatePassword(
                length, 
                upperCase.isSelected(), 
                lowerCase.isSelected(), 
                digits.isSelected(), 
                symbols.isSelected()
            );
            passwordLabel.setText(newPassword);
        } catch (IllegalArgumentException e) {
             passwordLabel.setText("Error: " + e.getMessage());
        }
    }
    
    /**
     * Public method to retrieve the generated password after the dialog is closed.
     * @return The generated password, or null if the dialog was cancelled.
     */
    public String getGeneratedPassword() {
        return generatedPassword;
    }
}