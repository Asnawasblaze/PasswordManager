package com.passwordmanager.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.passwordmanager.model.PasswordEntry;

public class PasswordDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordDAO.class);

    // SQL Statements adjusted to your schema's column names
    private static final String INSERT_PASSWORD_SQL =
            "INSERT INTO passwords (user_id, title, username, encrypted_password, nonce, encrypted_note, note_nonce) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_ALL_BY_USER_SQL =
            "SELECT password_id, title, username, encrypted_password, nonce, encrypted_note, note_nonce FROM passwords WHERE user_id = ?";
    private static final String DELETE_PASSWORD_SQL =
            "DELETE FROM passwords WHERE password_id = ? AND user_id = ?";
    // ADDED UPDATE functionality
    private static final String UPDATE_PASSWORD_SQL =
            "UPDATE passwords SET title = ?, username = ?, encrypted_password = ?, nonce = ?, encrypted_note = ?, note_nonce = ?, updated_at = CURRENT_TIMESTAMP WHERE password_id = ? AND user_id = ?";


    /**
     * Saves a new encrypted password entry to the database.
     */
    public int createEntry(PasswordEntry entry) {
        int entryId = -1;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(INSERT_PASSWORD_SQL, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setInt(1, entry.getUserId());
            preparedStatement.setString(2, entry.getTitle());

            // Correction is here: change 'preparedStatementStatement' to 'preparedStatement'
            preparedStatement.setString(3, entry.getUsernameEnc());    // username (encrypted service username)
            preparedStatement.setString(4, entry.getPasswordEnc());    // encrypted_password
            preparedStatement.setString(5, entry.getEntryNonce());     // nonce (for password)
            preparedStatement.setString(6, entry.getNoteEnc());        // encrypted_note
            preparedStatement.setString(7, entry.getNoteNonce());      // note_nonce

            if (preparedStatement.executeUpdate() > 0) {
                // ... rest of the method ...
            }
        } catch (SQLException e) {
            LOGGER.error("Error creating password entry: {}", e.getMessage(), e);
        }
        return entryId;
    }

    /**
     * Updates an existing encrypted password entry.
     */
    public boolean updateEntry(PasswordEntry entry) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(UPDATE_PASSWORD_SQL)) {

            preparedStatement.setString(1, entry.getTitle());
            preparedStatement.setString(2, entry.getUsernameEnc());
            preparedStatement.setString(3, entry.getPasswordEnc());
            preparedStatement.setString(4, entry.getEntryNonce());
            preparedStatement.setString(5, entry.getNoteEnc());
            preparedStatement.setString(6, entry.getNoteNonce());
            preparedStatement.setInt(7, entry.getId());        // WHERE password_id
            preparedStatement.setInt(8, entry.getUserId());    // WHERE user_id

            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Error updating entry with ID {}: {}", entry.getId(), e.getMessage(), e);
            return false;
        }
    }


    /**
     * Retrieves all encrypted password entries for a specific user.
     */
    public List<PasswordEntry> findAllByUserId(int userId) {
        List<PasswordEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(SELECT_ALL_BY_USER_SQL)) {

            preparedStatement.setInt(1, userId);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    PasswordEntry entry = new PasswordEntry();
                    entry.setId(rs.getInt("password_id"));
                    entry.setUserId(userId);
                    entry.setTitle(rs.getString("title"));

                    // Map encrypted fields
                    entry.setUsernameEnc(rs.getString("username"));
                    entry.setPasswordEnc(rs.getString("encrypted_password"));
                    entry.setNoteEnc(rs.getString("encrypted_note"));

                    // Map nonces
                    entry.setEntryNonce(rs.getString("nonce"));
                    entry.setNoteNonce(rs.getString("note_nonce"));

                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error retrieving password entries for user {}: {}", userId, e.getMessage(), e);
        }
        return entries;
    }

    /**
     * Deletes a password entry.
     */
    public boolean deleteEntry(int entryId, int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(DELETE_PASSWORD_SQL)) {

            preparedStatement.setInt(1, entryId);
            preparedStatement.setInt(2, userId);

            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Error deleting entry with ID {}: {}", entryId, e.getMessage(), e);
            return false;
        }
    }
}