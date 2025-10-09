package com.passwordmanager.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Database Manager for Supabase PostgreSQL Connection
 * Handles connection pooling and database operations
 */
public class DatabaseManager {

    // ========================================
    // DATABASE CONFIGURATION
    // ========================================
    // TODO: REPLACE THESE WITH YOUR SUPABASE CREDENTIALS
    // Get from: Supabase Dashboard → Settings → Database → Connection string (JDBC tab)

    private static final String DB_HOST = "aws-1-ap-south-1.pooler.supabase.com"; // Get this from Supabase
    private static final String DB_PORT = "6543";  // Port for the Pooler
    private static final String DB_NAME = "postgres";
    private static final String DB_USER = "postgres.qktasrecuazcnrfcraos";
    private static final String DB_PASSWORD = "vgAwGWIrHhiiB6yC";

    // New DB_URL construction for the Pooler
    private static final String DB_URL = String.format(
            "jdbc:postgresql://%s:%s/%s?sslmode=require&prepareThreshold=0",
            DB_HOST, DB_PORT, DB_NAME
    );
    // Singleton instance
    private static DatabaseManager instance;
    private Connection connection;

    // ... (Private constructor, getInstance() method - NO CHANGES) ...

    /**
     * Private constructor for singleton pattern
     */
    private DatabaseManager() {
        try {
            // Load PostgreSQL JDBC Driver
            Class.forName("org.postgresql.Driver");
            System.out.println("✓ PostgreSQL JDBC Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ PostgreSQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    /**
     * Get singleton instance of DatabaseManager
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }


    /**
     * Establish connection to database
     */
    public Connection getConnection() throws SQLException {
        // Check if connection is closed or null
        if (connection == null || connection.isClosed()) {
            try {
                System.out.println("→ Connecting to Supabase PostgreSQL...");
                // Print URL without password for security
                System.out.println("→ URL: " + DB_URL);

                // Pass the clean URL, username, and password separately
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

                System.out.println("✓ Connected to database successfully!");
                return connection;

            } catch (SQLException e) {
                System.err.println("✗ Failed to connect to database!");
                System.err.println("Error: " + e.getMessage());
                throw e;
            }
        }
        return connection;
    }

    // ... (testConnection(), closeConnection(), and main() methods - NO CHANGES) ...
    /**
     * Test database connection and query tables
     */
    /**
     * Test database connection and query tables
     */
    public boolean testConnection() {
        // Use try-with-resources for Connection and Statement
        try (Connection conn = getConnection();
            Statement stmt = conn.createStatement()) {

            System.out.println("\n=== Testing Database Connection ===\n");

            // Test 1: Check connection
            if (conn != null && !conn.isClosed()) {
                System.out.println("✓ Database connection is active");
            }

            // Test 2: Query database version
            // DECLARE 'rs' ONCE HERE
            ResultSet rs = stmt.executeQuery("SELECT version()");
            if (rs.next()) {
                String version = rs.getString(1);
                System.out.println("✓ PostgreSQL Version: " + version.substring(0, 50) + "...");
            }
            rs.close(); // Good practice: close the ResultSet when done

            // Test 3: List tables
            // REUSE 'rs' - DO NOT re-declare the type
            rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                            "WHERE table_schema = 'public' ORDER BY table_name"
            );

            System.out.println("\n✓ Tables in database:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("table_name"));
            }
            rs.close(); // Close the ResultSet

            // Test 4: Count users
            try {
                // REUSE 'rs'
                rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
                if (rs.next()) {
                    System.out.println("\n✓ Users table: " + rs.getInt("count") + " user(s)");
                }
                rs.close(); // Close the ResultSet
            } catch (SQLException e) {
                System.out.println("\n⚠ Users table not found (Skipping count). Ensure Phase 2.2 is complete.");
            }

            // Test 5: Count passwords
            try {
                // REUSE 'rs'
                rs = stmt.executeQuery("SELECT COUNT(*) as count FROM passwords");
                if (rs.next()) {
                    System.out.println("✓ Passwords table: " + rs.getInt("count") + " password(s)");
                }
                rs.close(); // Close the ResultSet
            } catch (SQLException e) {
                System.out.println("⚠ Passwords table not found (Skipping count). Ensure Phase 2.2 is complete.");
            }

            System.out.println("\n=== All Tests Passed! ===\n");
            return true;

        } catch (SQLException e) {
            System.err.println("\n✗ Connection test failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Close database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("✗ Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("Password Manager - Database Test");
        System.out.println("=================================\n");

        DatabaseManager dbManager = DatabaseManager.getInstance();

        // Test connection
        boolean success = dbManager.testConnection();

        if (success) {
            System.out.println("✓ Database is ready for Password Manager!");
        } else {
            System.out.println("✗ Please check your database configuration");
        }

        // Clean up
        dbManager.closeConnection();
    }
}