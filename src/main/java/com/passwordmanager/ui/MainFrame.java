package com.passwordmanager.ui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import javax.swing.BorderFactory; // Use javax.swing.Timer for event dispatch thread safety
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import com.passwordmanager.model.PasswordEntry;
import com.passwordmanager.model.User;
import com.passwordmanager.service.PasswordService;
import com.passwordmanager.util.Pbkdf2HashUtil;

public class MainFrame extends JFrame {

    private final User loggedInUser;
    private final byte[] masterKeyBytes;
    private final PasswordService passwordService = new PasswordService();

    // UI Components
    private JTable passwordTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton decryptButton;
    private JButton copyButton;
    private JButton deleteButton;
    private JSplitPane splitPane;
    
    // Security Timers
    private static final int CLIPBOARD_CLEAR_DELAY_MS = 30000; // 30 seconds
    private static final int INACTIVITY_TIMEOUT_MS = 300000; // 5 minutes
    private Timer inactivityTimer;

    public MainFrame(User user, byte[] masterKeyBytes) {
        this.loggedInUser = user;
        this.masterKeyBytes = masterKeyBytes;

        setTitle("Password Vault - " + user.getUsername());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        initUI();
        loadPasswordData(); 

        // Start Security Features
        startInactivityTimer();

        // Enable AWT event logging for better inactivity detection
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> resetInactivityTimer(), 
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        
        JPanel sidebar = createSidebar();
        JPanel mainContent = createMainContentPanel();
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, mainContent);
        splitPane.setDividerLocation(200); 
        add(splitPane, BorderLayout.CENTER);
    }
    
    // --- UI Creation Methods ---
    
    private JPanel createSidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel vaultLabel = new JLabel("SECURE VAULT");
        vaultLabel.setFont(new Font("Arial", Font.BOLD, 18));
        vaultLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        vaultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(vaultLabel);
        
        // Initialize Buttons
        addButton = new JButton("+ Add New Entry");
        decryptButton = new JButton("Decrypt & View");
        copyButton = new JButton("Copy Password");
        deleteButton = new JButton("Delete Entry");
        
        // Styling
        styleSidebarButton(addButton, new Color(50, 150, 250));
        styleSidebarButton(decryptButton, null);
        styleSidebarButton(copyButton, null);
        styleSidebarButton(deleteButton, new Color(200, 50, 50));

        panel.add(addButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(decryptButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(copyButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(deleteButton);
        panel.add(Box.createVerticalGlue());

        // Add Listeners (Phase 7 Integration)
        addButton.addActionListener(e -> showAddPasswordDialog());
        decryptButton.addActionListener(e -> decryptSelectedPassword());
        copyButton.addActionListener(e -> copySelectedPassword());
        deleteButton.addActionListener(e -> deleteSelectedEntry()); // Implement this method later
        
        return panel;
    }

    private void styleSidebarButton(JButton button, Color backgroundColor) {
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        if (backgroundColor != null) {
            button.setBackground(backgroundColor);
            button.setForeground(Color.WHITE);
        }
    }
    
    private JPanel createMainContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columnNames = {"ID", "Title", "Username (Encrypted)", "Note (Encrypted)", "Created"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
            @Override // Ensure ID column holds Integer type
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };
        passwordTable = new JTable(tableModel);
        passwordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        passwordTable.setRowHeight(25);
        passwordTable.setShowVerticalLines(false);
        passwordTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        JScrollPane scrollPane = new JScrollPane(passwordTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    // --- Data and Business Logic Methods ---
    
    public void loadPasswordData() {
        tableModel.setRowCount(0);
        
        List<PasswordEntry> entries = passwordService.getEncryptedEntries(loggedInUser.getId());
        
        for (PasswordEntry entry : entries) {
            // Note: Displaying the ciphertext for security
            tableModel.addRow(new Object[]{
                entry.getId(),
                entry.getTitle(),
                entry.getUsernameEnc() != null ? entry.getUsernameEnc().substring(0, Math.min(entry.getUsernameEnc().length(), 20)) + "..." : "",
                entry.getNoteEnc() != null ? entry.getNoteEnc().substring(0, Math.min(entry.getNoteEnc().length(), 20)) + "..." : "",
                "..." 
            });
        }
    }

    private void showAddPasswordDialog() {
        AddPasswordDialog addDialog = new AddPasswordDialog(this, loggedInUser, masterKeyBytes);
        addDialog.setVisible(true);
    }
    
    private PasswordEntry getSelectedEntry() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an entry.", "Select Entry", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int entryId = (int) tableModel.getValueAt(selectedRow, 0); 
        
        // This is necessary because the table only stores truncated/encrypted strings
        List<PasswordEntry> entries = passwordService.getEncryptedEntries(loggedInUser.getId());
        return entries.stream()
            .filter(e -> e.getId() == entryId)
            .findFirst()
            .orElse(null);
    }
    
    // --- Phase 7 Decryption & Copy Logic ---

    private void decryptSelectedPassword() {
        PasswordEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null) return;

        // Decrypt password (and username/note for full view)
        String decryptedPassword = passwordService.decryptPassword(selectedEntry, masterKeyBytes);

        JOptionPane.showMessageDialog(this, 
            "Title: " + selectedEntry.getTitle() + "\n" +
            "Password: " + decryptedPassword, 
            "Decrypted Password", JOptionPane.INFORMATION_MESSAGE);
    }

    private void copySelectedPassword() {
        PasswordEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null) return;

        String decryptedPassword = passwordService.decryptPassword(selectedEntry, masterKeyBytes);
        
        // Copy to Clipboard
        StringSelection stringSelection = new StringSelection(decryptedPassword);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        
        JOptionPane.showMessageDialog(this, "Password copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
        
        // Trigger clipboard auto-clear
        startClipboardClearTimer(); 
    }
    
    private void deleteSelectedEntry() {
        PasswordEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete the entry: " + selectedEntry.getTitle() + "?", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            if (passwordService.deleteEntry(selectedEntry.getId(), loggedInUser.getId())) {
                JOptionPane.showMessageDialog(this, "Entry deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadPasswordData(); // Refresh table
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete entry.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Phase 7 Security Features ---

    private void startClipboardClearTimer() {
        Timer timer = new Timer(CLIPBOARD_CLEAR_DELAY_MS, e -> {
            try {
                StringSelection emptySelection = new StringSelection("");
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(emptySelection, null);
            } catch (Exception ex) {
                // Ignore silent failure
            }
        });
        timer.setRepeats(false); 
        timer.start();
    }
    
    private void startInactivityTimer() {
        inactivityTimer = new Timer(INACTIVITY_TIMEOUT_MS, e -> autoLock());
        inactivityTimer.setRepeats(false);
        inactivityTimer.start();
    }

    private void resetInactivityTimer() {
        if (inactivityTimer != null && inactivityTimer.isRunning()) {
            inactivityTimer.restart();
        }
    }

    private void autoLock() {
        JOptionPane.showMessageDialog(this, "Application locked due to inactivity.", "Security Lock", JOptionPane.WARNING_MESSAGE);
        
        // Clean up and log out
        if (inactivityTimer != null) inactivityTimer.stop();
        dispose();
        
        // Return to login screen
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.setVisible(true);
        });
    }

    // Override processEvent to catch global user activity and reset the timer
    // NOTE: Requires Toolkit.getDefaultToolkit().addAWTEventListener() call in the constructor!
    @Override
    public void processEvent(AWTEvent e) {
        if (e.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED ||
            e.getID() == java.awt.event.KeyEvent.KEY_PRESSED) {
            resetInactivityTimer();
        }
        super.processEvent(e);
    }
}