package com.passwordmanager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.passwordmanager.ui.LoginDialog;
import javax.swing.SwingUtilities;

public class AppLauncher {

    public static void main(String[] args) {
        // Use a modern, dark theme for a "premium" look
        // You can switch to FlatLaf.install(new FlatLightLaf()); for a lighter theme
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            // Instantiate and display the main login window
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.setVisible(true);
        });
    }
}