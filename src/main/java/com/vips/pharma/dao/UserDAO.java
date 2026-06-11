package com.vips.pharma.dao;

import com.vips.pharma.model.Role;
import com.vips.pharma.model.User;
import com.vips.pharma.util.DatabaseUtil;
import com.vips.pharma.util.PasswordUtil;
import com.vips.pharma.util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access layer for the {@code users} table.
 *
 * All mutating operations (add / update / delete) write an audit entry
 * stamped with the currently logged-in administrator's username.
 */
public class UserDAO {

    // ── Read ──────────────────────────────────────────────────────────────

    /** Returns all users ordered by username. */
    public List<User> getAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role, active FROM users ORDER BY username";
        try (Statement st = DatabaseUtil.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /** Find a user by exact username (case-insensitive). Returns null if not found. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, role, active FROM users WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // ── Authentication ────────────────────────────────────────────────────

    /**
     * Authenticates a login attempt.
     *
     * @return the matching {@link User} if credentials are valid and the
     *         account is active; {@code null} otherwise.
     */
    public User authenticate(String username, String plainPassword) throws SQLException {
        User user = findByUsername(username);
        if (user == null || !user.isActive()) return null;
        return PasswordUtil.verify(plainPassword, user.getPasswordHash()) ? user : null;
    }

    // ── Create ────────────────────────────────────────────────────────────

    /**
     * Inserts a new user.  The {@code plainPassword} is hashed before storage.
     */
    public void add(String username, String plainPassword, Role role) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, 1)";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, PasswordUtil.hash(plainPassword));
            ps.setString(3, role.name());
            ps.executeUpdate();
        }
        logAudit("CREATED USER: " + username + " | Role: " + role.displayName());
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Updates a user's role and active status.
     * Pass {@code null} for {@code newPlainPassword} to leave password unchanged.
     */
    public void update(User user, String newPlainPassword) throws SQLException {
        if (newPlainPassword != null && !newPlainPassword.isBlank()) {
            String sql = "UPDATE users SET username=?, password_hash=?, role=?, active=? WHERE id=?";
            try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
                ps.setString(1, user.getUsername().trim());
                ps.setString(2, PasswordUtil.hash(newPlainPassword));
                ps.setString(3, user.getRole().name());
                ps.setInt(4, user.isActive() ? 1 : 0);
                ps.setInt(5, user.getId());
                ps.executeUpdate();
            }
            logAudit("UPDATED USER (with password change): " + user.getUsername());
        } else {
            String sql = "UPDATE users SET username=?, role=?, active=? WHERE id=?";
            try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
                ps.setString(1, user.getUsername().trim());
                ps.setString(2, user.getRole().name());
                ps.setInt(3, user.isActive() ? 1 : 0);
                ps.setInt(4, user.getId());
                ps.executeUpdate();
            }
            logAudit("UPDATED USER: " + user.getUsername()
                     + " | Role: " + user.getRole().displayName()
                     + " | Active: " + user.isActive());
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────

    /** Permanently deletes a user record. */
    public void delete(User user) throws SQLException {
        String sql = "DELETE FROM users WHERE id=?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
        }
        logAudit("DELETED USER: " + user.getUsername());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private User map(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role")),
            rs.getInt("active") == 1
        );
    }

    private void logAudit(String action) throws SQLException {
        String sql = "INSERT INTO audit_logs (action, username, date_time) VALUES (?, ?, datetime('now','localtime'))";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, SessionManager.getInstance().getUsername());
            ps.executeUpdate();
        }
    }
}
