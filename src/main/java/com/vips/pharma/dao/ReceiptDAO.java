package com.vips.pharma.dao;

import com.vips.pharma.model.AuditLog;
import com.vips.pharma.model.CartItem;
import com.vips.pharma.model.Receipt;
import com.vips.pharma.util.DatabaseUtil;
import com.vips.pharma.util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReceiptDAO {

    public void save(Receipt r) throws SQLException {
        String sql = "INSERT INTO receipts (med_name, quantity, total_amount, date_time, processed_by) "
                   + "VALUES (?, ?, ?, datetime('now','localtime'), ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getMedName());
            ps.setInt(2, r.getQuantity());
            ps.setDouble(3, r.getTotalAmount());
            ps.setString(4, SessionManager.getInstance().getUsername());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) r.setId(keys.getInt(1));
            r.setProcessedBy(SessionManager.getInstance().getUsername());
        }
        // Audit the sale
        String auditSql = "INSERT INTO audit_logs (action, username, date_time) VALUES (?, ?, datetime('now','localtime'))";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(auditSql)) {
            ps.setString(1, "PROCESSED SALE — Receipt #" + r.getId()
                            + " | Items: " + r.getMedName()
                            + " | Total: ₱" + String.format("%.2f", r.getTotalAmount()));
            ps.setString(2, SessionManager.getInstance().getUsername());
            ps.executeUpdate();
        }
    }

    /** Save individual line items for a receipt (called after save()). */
    public void saveItems(int receiptId, List<CartItem> items) throws SQLException {
        String sql = "INSERT INTO receipt_items (receipt_id, med_name, quantity, unit_price, total) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            for (CartItem ci : items) {
                ps.setInt(1, receiptId);
                ps.setString(2, ci.getMedName());
                ps.setInt(3, ci.getQuantity());
                ps.setDouble(4, ci.getUnitPrice());
                ps.setDouble(5, ci.getTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Fetch line items for a receipt; returns empty list if none stored (legacy receipts). */
    public List<CartItem> getItemsByReceiptId(int receiptId) throws SQLException {
        List<CartItem> list = new ArrayList<>();
        String sql = "SELECT med_name, quantity, unit_price FROM receipt_items WHERE receipt_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, receiptId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new CartItem(0, rs.getString("med_name"),
                        rs.getInt("quantity"), rs.getDouble("unit_price")));
            }
        }
        return list;
    }

    public List<Receipt> getAll() throws SQLException {
        List<Receipt> list = new ArrayList<>();
        String sql = "SELECT id, med_name, quantity, total_amount, date_time, processed_by "
                   + "FROM receipts ORDER BY date_time DESC";
        try (Statement st = DatabaseUtil.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Receipt r = new Receipt(
                        rs.getInt("id"),
                        rs.getString("med_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("total_amount"),
                        rs.getString("date_time"));
                r.setProcessedBy(rs.getString("processed_by"));
                list.add(r);
            }
        }
        return list;
    }

    public List<AuditLog> getAuditLogs() throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT id, action, username, date_time FROM audit_logs ORDER BY date_time DESC";
        try (Statement st = DatabaseUtil.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new AuditLog(
                        rs.getInt("id"),
                        rs.getString("action"),
                        rs.getString("username"),
                        rs.getString("date_time")));
            }
        }
        return list;
    }
}
