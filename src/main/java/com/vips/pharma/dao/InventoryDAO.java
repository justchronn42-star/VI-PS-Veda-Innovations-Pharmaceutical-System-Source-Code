package com.vips.pharma.dao;

import com.vips.pharma.model.Medicine;
import com.vips.pharma.util.DatabaseUtil;
import com.vips.pharma.util.SessionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    public List<Medicine> getAll() throws SQLException {
        List<Medicine> list = new ArrayList<>();
        String sql = "SELECT id, name, stock, price, expiry_date FROM inventory ORDER BY name";
        try (Statement st = DatabaseUtil.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String expiryStr = rs.getString("expiry_date");
                LocalDate expiry = (expiryStr != null && !expiryStr.isBlank())
                        ? LocalDate.parse(expiryStr) : null;
                list.add(new Medicine(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("stock"),
                        rs.getDouble("price"),
                        expiry));
            }
        }
        return list;
    }

    public void add(Medicine m) throws SQLException {
        String sql = "INSERT INTO inventory (name, stock, price, expiry_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setInt(2, m.getStock());
            ps.setDouble(3, m.getPrice());
            ps.setString(4, m.getExpiryDate() != null ? m.getExpiryDate().toString() : null);
            ps.executeUpdate();
        }
        logAudit("ADDED NEW MEDICINE: " + m.getName()
                + " (Stock: " + m.getStock() + ", Price: \u20b1" + m.getPrice()
                + ", Expiry: " + (m.getExpiryDate() != null ? m.getExpiryDate() : "N/A") + ")");
    }

    public void update(Medicine m) throws SQLException {
        String sql = "UPDATE inventory SET name=?, stock=?, price=?, expiry_date=? WHERE id=?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setInt(2, m.getStock());
            ps.setDouble(3, m.getPrice());
            ps.setString(4, m.getExpiryDate() != null ? m.getExpiryDate().toString() : null);
            ps.setInt(5, m.getId());
            ps.executeUpdate();
        }
        logAudit("UPDATED MEDICINE: " + m.getName()
                + " (Stock: " + m.getStock() + ", Price: \u20b1" + m.getPrice()
                + ", Expiry: " + (m.getExpiryDate() != null ? m.getExpiryDate() : "N/A") + ")");
    }

    public void delete(int id, String name) throws SQLException {
        String sql = "DELETE FROM inventory WHERE id=?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        logAudit("DELETED MEDICINE: " + name);
    }

    public void deductStock(int id, int qty) throws SQLException {
        String sql = "UPDATE inventory SET stock = stock - ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
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
