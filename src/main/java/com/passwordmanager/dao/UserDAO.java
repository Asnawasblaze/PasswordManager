package com.passwordmanager.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.passwordmanager.model.User;

public class UserDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDAO.class);

    // SQL Statements adjusted to your schema's column names
    private static final String INSERT_USER_SQL =
            "INSERT INTO users (username, master_password_hash, salt, totp_secret) VALUES (?, ?, ?, ?) RETURNING user_id";
    private static final String SELECT_USER_BY_USERNAME_SQL =
            "SELECT user_id, username, master_password_hash, salt, totp_secret FROM users WHERE username = ?";
    private static final String UPDATE_HASH_SQL =
            "UPDATE users SET master_password_hash = ?, salt = ? WHERE user_id = ?";
    private static final String UPDATE_TOTP_SQL =
            "UPDATE users SET totp_secret = ? WHERE user_id = ?";


    /**
     * Creates a new user entry in the database.
     * @param user The User model object containing hashed password and (encrypted) TOTP info.
     * @return The generated user_id, or -1 on failure.
     */
    public int createUser(User user, String totpToStore) {
        int userId = -1;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(INSERT_USER_SQL)) {

            LOGGER.info("Attempting to create user with username: {}", user.getUsername());

            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getMasterHash()); // master_password_hash
            preparedStatement.setString(3, user.getMasterSalt());  // salt
            preparedStatement.setString(4, totpToStore); // totp_secret (encrypted or plain based on caller)

            // Use INSERT ... RETURNING to reliably fetch generated id (works well with Supabase/Postgres pooler)
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt(1); // first column is user_id
                    user.setId(userId);
                    LOGGER.info("User created with user_id: {}", userId);
                } else {
                    LOGGER.warn("User creation query executed but no user_id returned.");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error creating user (INSERT users): {}", e.getMessage(), e);
        }
        return userId;
    }

    /**
 * Debug method to list all users in the database.
 * @return String listing all user ids and usernames.
 */
public String listAllUsers() {
    StringBuilder sb = new StringBuilder();
    try (Connection conn = DatabaseManager.getInstance().getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement("SELECT user_id, username FROM users")) {

        try (ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                sb.append("User ID: ").append(rs.getInt("user_id"))
                .append(", Username: ").append(rs.getString("username"))
                .append("\n");
            }
        }
    } catch (SQLException e) {
        LOGGER.error("Error listing all users: {}", e.getMessage(), e);
        return "Error retrieving users: " + e.getMessage();
    }
    return sb.length() > 0 ? sb.toString() : "No users found in database.";
}


    /**
     * Retrieves a user by username for authentication.
     * @param username The username to look up.
     * @return An Optional containing the User object if found, or empty otherwise.
     */
    public Optional<User> findUserByUsername(String username) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(SELECT_USER_BY_USERNAME_SQL)) {

            LOGGER.info("Searching for user with username: {}", username);

            preparedStatement.setString(1, username);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setMasterHash(rs.getString("master_password_hash"));
                    user.setMasterSalt(rs.getString("salt"));
                    user.setTotpSecretEnc(rs.getString("totp_secret"));

                    LOGGER.info("User found: id={}, username={}", user.getId(), user.getUsername());

                    return Optional.of(user);
                } else {
                    LOGGER.info("No user found with username: {}", username);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error retrieving user: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }
}