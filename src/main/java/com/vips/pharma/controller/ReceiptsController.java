package com.vips.pharma.controller;

import com.vips.pharma.dao.ReceiptDAO;
import com.vips.pharma.model.CartItem;
import com.vips.pharma.model.Receipt;
import com.vips.pharma.report.ReportGenerator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ReceiptsController implements Initializable {

    @FXML private TableView<Receipt>            receiptsTable;
    @FXML private TableColumn<Receipt, Integer> colId, colQty;
    @FXML private TableColumn<Receipt, String>  colMed, colTotal, colDate, colProcessedBy;
    @FXML private Label lblTotalSales, lblTotalRevenue;

    private final ReceiptDAO dao = new ReceiptDAO();
    private final ObservableList<Receipt> items = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMed.setCellValueFactory(new PropertyValueFactory<>("medName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        colProcessedBy.setCellValueFactory(new PropertyValueFactory<>("processedBy"));

        colTotal.setCellValueFactory(cd ->
                new SimpleStringProperty(String.format("₱%.2f", cd.getValue().getTotalAmount())));

        receiptsTable.setItems(items);
        loadData();
    }

    private void loadData() {
        try {
            List<Receipt> list = dao.getAll();
            items.setAll(list);
            lblTotalSales.setText("Total Transactions: " + list.size());
            double revenue = list.stream().mapToDouble(Receipt::getTotalAmount).sum();
            lblTotalRevenue.setText(String.format("Total Revenue: ₱%.2f", revenue));
        } catch (Exception e) {
            showAlert("DB Error", e.getMessage());
        }
    }

    @FXML
    private void reprintSelected() {
        Receipt r = receiptsTable.getSelectionModel().getSelectedItem();
        if (r == null) { showAlert("No Selection", "Please select a receipt to reprint."); return; }
        try {
            List<CartItem> cartItems = new java.util.ArrayList<>(dao.getItemsByReceiptId(r.getId()));

            if (cartItems.isEmpty()) {
                // Legacy receipt — parse summary string e.g. "MED A x2, MED B x1"
                String[] parts = r.getMedName().split(",\\s*");
                int totalQty = Math.max(r.getQuantity(), 1);
                for (String part : parts) {
                    part = part.trim();
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("^(.+?)\\s+x(\\d+)$").matcher(part);
                    if (m.matches()) {
                        String name = m.group(1).trim();
                        int qty = Integer.parseInt(m.group(2));
                        double unitPrice = r.getTotalAmount() / totalQty;
                        cartItems.add(new CartItem(0, name, qty, unitPrice));
                    } else {
                        cartItems.add(new CartItem(0, part, 1, r.getTotalAmount()));
                    }
                }
            }

            String path = ReportGenerator.generateReceipt(r, cartItems);
            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(path)); }
            catch (Exception ignore) {}
        } catch (Exception e) {
            showAlert("Print Error", e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
