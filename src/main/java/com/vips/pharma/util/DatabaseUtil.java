package com.vips.pharma.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {

    private static final String DB_PATH = "vips_pharma.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            connection.setAutoCommit(true);
            initSchema();
        }
        return connection;
    }

    private static void initSchema() throws SQLException {
        var stmt = connection.createStatement();

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS inventory (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT    NOT NULL,
                stock       INTEGER NOT NULL,
                price       REAL    NOT NULL,
                expiry_date TEXT
            )
        """);

        // Migration: add expiry_date column if it doesn't exist (for existing databases)
        try {
            stmt.executeUpdate("ALTER TABLE inventory ADD COLUMN expiry_date TEXT");
        } catch (SQLException ignored) {
            // Column already exists — that's fine
        }

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS receipts (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                med_name     TEXT    NOT NULL,
                quantity     INTEGER NOT NULL,
                total_amount REAL    NOT NULL,
                date_time    TEXT    NOT NULL,
                processed_by TEXT    NOT NULL DEFAULT 'SYSTEM'
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS receipt_items (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                receipt_id   INTEGER NOT NULL,
                med_name     TEXT    NOT NULL,
                quantity     INTEGER NOT NULL,
                unit_price   REAL    NOT NULL,
                total        REAL    NOT NULL,
                FOREIGN KEY (receipt_id) REFERENCES receipts(id)
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS audit_logs (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                action    TEXT    NOT NULL,
                username  TEXT    NOT NULL DEFAULT 'SYSTEM',
                date_time TEXT    NOT NULL
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT    NOT NULL UNIQUE,
                password_hash TEXT    NOT NULL,
                role          TEXT    NOT NULL,
                active        INTEGER NOT NULL DEFAULT 1
            )
        """);

        var rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
        if (rs.next() && rs.getInt(1) == 0) seedDefaultAdmin();
        rs.close();
        stmt.close();
    }

    private static void seedDefaultAdmin() throws SQLException {
        String hash = PasswordUtil.hash("admin123");
        var ps = connection.prepareStatement(
            "INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, 'ADMIN', 1)"
        );
        ps.setString(1, "admin");
        ps.setString(2, hash);
        ps.executeUpdate();
        ps.close();
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
