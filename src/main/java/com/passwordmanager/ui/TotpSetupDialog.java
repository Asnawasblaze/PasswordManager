package com.passwordmanager.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.google.zxing.WriterException;
import com.passwordmanager.util.QrCodeUtil;

public class TotpSetupDialog extends JDialog {

    private final String secret;
    private final String qrCodeUri;

    public TotpSetupDialog(JDialog parent, String secret, String qrCodeUri) {
        super(parent, "Setup Two-Factor Authentication", true);
        this.secret = secret;
        this.qrCodeUri = qrCodeUri;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel title = new JLabel("Setup Two-Factor Authentication");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        header.add(title);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        // QR image
        JLabel qrLabel = new JLabel();
        try {
            BufferedImage img = QrCodeUtil.generateQr(qrCodeUri, 220);
            qrLabel.setIcon(new ImageIcon(img));
        } catch (WriterException ex) {
            qrLabel.setText("Unable to render QR. Use manual key below.");
        }

        gbc.gridx = 0; gbc.gridy = 0;
        body.add(qrLabel, gbc);

        // Manual key
        gbc.gridy = 1;
        body.add(new JLabel("Manual Entry Key (if QR code doesn't work)"), gbc);

        gbc.gridy = 2;
        JTextField keyField = new JTextField(secret, 24);
        keyField.setEditable(false);
        keyField.setToolTipText("Enter this secret manually in your authenticator if scanning fails.");
        body.add(keyField, gbc);

        add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backBtn = new JButton("Back to Registration");
        JButton doneBtn = new JButton("I've Added the Account");
        actions.add(backBtn);
        actions.add(doneBtn);
        add(actions, BorderLayout.SOUTH);

        backBtn.addActionListener(e -> dispose());
        doneBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "2FA setup complete. You can now log in with your TOTP code.", "Done", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
    }
}
