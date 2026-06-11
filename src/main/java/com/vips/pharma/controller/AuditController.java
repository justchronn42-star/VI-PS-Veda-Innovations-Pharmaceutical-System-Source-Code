package com.vips.pharma.controller;

import com.vips.pharma.dao.ReceiptDAO;
import com.vips.pharma.model.AuditLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AuditController implements Initializable {

    @FXML private TableView<AuditLog>           auditTable;
    @FXML private TableColumn<AuditLog, Integer> colId;
    @FXML private TableColumn<AuditLog, String>  colAction, colUsername, colDateTime;

    private final ReceiptDAO dao = new ReceiptDAO();
    private final ObservableList<AuditLog> items = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDateTime.setCellValueFactory(new PropertyValueFactory<>("dateTime"));

        auditTable.setItems(items);
        loadData();
    }

    private void loadData() {
        try {
            items.setAll(dao.getAuditLogs());
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            a.setTitle("DB Error"); a.setHeaderText(null); a.showAndWait();
        }
    }
}
