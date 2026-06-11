package com.vips.pharma.controller;

import com.vips.pharma.dao.InventoryDAO;
import com.vips.pharma.model.Medicine;
import com.vips.pharma.model.Role;
import com.vips.pharma.report.ReportGenerator;
import com.vips.pharma.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class InventoryController implements Initializable {

    @FXML private TableView<Medicine>            inventoryTable;
    @FXML private TableColumn<Medicine, Integer> colId, colStock;
    @FXML private TableColumn<Medicine, String>  colName, colPrice, colExpiry;
    @FXML private TableColumn<Medicine, Void>    colActions;
    @FXML private TextField tfSearch;
    @FXML private Label lblTotalItems, lblLowStock, lblExpired;
    @FXML private Button btnAddMedicine;

    private final InventoryDAO dao = new InventoryDAO();
    private final ObservableList<Medicine> items = FXCollections.observableArrayList();
    private List<Medicine> allData; // full unfiltered list

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Role role = SessionManager.getInstance().getCurrentUser().getRole();
        boolean canEdit = role.canEditInventory();

        if (btnAddMedicine != null) {
            btnAddMedicine.setVisible(canEdit);
            btnAddMedicine.setManaged(canEdit);
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        colPrice.setCellValueFactory(cd ->
                new SimpleStringProperty(String.format("\u20b1%.2f", cd.getValue().getPrice())));

        colExpiry.setCellValueFactory(cd -> {
            LocalDate d = cd.getValue().getExpiryDate();
            if (d == null) return new SimpleStringProperty("—");
            String label = d.format(DATE_FMT);
            if (cd.getValue().isExpired()) label += " ⚠ EXPIRED";
            return new SimpleStringProperty(label);
        });

        colStock.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(item));
                Medicine m = getTableView().getItems().get(getIndex());
                if (!m.isExpired()) {
                    setStyle(item < 20 ? "-fx-text-fill:#e74c3c; -fx-font-weight:bold;" : "");
                } else {
                    setStyle("");
                }
            }
        });

        inventoryTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Medicine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isExpired()) {
                    setStyle("-fx-background-color: #ffcccc;");
                } else {
                    setStyle("");
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("\u270f Edit");
            private final Button btnDelete = new Button("\uD83D\uDDD1 Del");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-sm-primary");
                btnDelete.getStyleClass().add("btn-sm-danger");
                btnEdit.setOnAction(e -> showEditDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteItem(getTableView().getItems().get(getIndex())));
                btnEdit.setVisible(canEdit);   btnEdit.setManaged(canEdit);
                btnDelete.setVisible(canEdit); btnDelete.setManaged(canEdit);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Bind table directly to ObservableList — same as Sales History, enables native column sorting
        inventoryTable.setItems(items);

        // Search filters by repopulating items from the full list
        tfSearch.textProperty().addListener((obs, o, n) -> applyFilter(n));

        loadData();
    }

    private void applyFilter(String query) {
        if (allData == null) return;
        if (query == null || query.isBlank()) {
            items.setAll(allData);
        } else {
            String lower = query.toLowerCase();
            items.setAll(allData.stream()
                    .filter(m -> m.getName().toLowerCase().contains(lower))
                    .collect(Collectors.toList()));
        }
    }

    private void loadData() {
        try {
            allData = dao.getAll();
            String query = tfSearch != null ? tfSearch.getText() : "";
            applyFilter(query);
            lblTotalItems.setText("Items: " + allData.size());
            long low = allData.stream().filter(m -> m.getStock() < 20 && !m.isExpired()).count();
            lblLowStock.setText("Low Stock: " + low);
            long expired = allData.stream().filter(Medicine::isExpired).count();
            if (lblExpired != null) lblExpired.setText("Expired: " + expired);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
        }
    }

    @FXML
    private void showAddDialog() {
        if (!SessionManager.getInstance().getCurrentUser().getRole().canEditInventory()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Your role does not allow adding medicines.");
            return;
        }
        showDialog("Add Medicine", new Medicine(0, "", 0, 0.0), true);
    }

    private void showEditDialog(Medicine m) {
        if (!SessionManager.getInstance().getCurrentUser().getRole().canEditInventory()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Your role does not allow editing medicines.");
            return;
        }
        showDialog("Edit Medicine", m, false);
    }

    private void showDialog(String title, Medicine m, boolean isNew) {
        Dialog<Medicine> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField tfName   = new TextField(m.getName());
        TextField tfStock  = new TextField(isNew ? "" : String.valueOf(m.getStock()));
        TextField tfPrice  = new TextField(isNew ? "" : String.format("%.2f", m.getPrice()));
        TextField tfExpiry = new TextField(
                m.getExpiryDate() != null ? m.getExpiryDate().format(DATE_FMT) : "");
        tfName.setPromptText("Medicine name");
        tfStock.setPromptText("Stock quantity");
        tfPrice.setPromptText("Unit price");
        tfExpiry.setPromptText("MM/DD/YYYY  (leave blank if none)");

        VBox form = new VBox(10,
                new Label("Medicine Name:"), tfName,
                new Label("Stock:"),          tfStock,
                new Label("Price (\u20b1):"),  tfPrice,
                new Label("Expiry Date:"),     tfExpiry);
        form.setPadding(new Insets(16));
        form.setPrefWidth(300);
        dlg.getDialogPane().setContent(form);

        dlg.setResultConverter(bt -> {
            if (bt == saveBtn) {
                try {
                    m.setName(tfName.getText().trim().toUpperCase());
                    m.setStock(Integer.parseInt(tfStock.getText().trim()));
                    m.setPrice(Double.parseDouble(tfPrice.getText().trim()));

                    String expiryText = tfExpiry.getText().trim();
                    if (expiryText.isBlank()) {
                        m.setExpiryDate(null);
                    } else {
                        try {
                            m.setExpiryDate(LocalDate.parse(expiryText, DATE_FMT));
                        } catch (DateTimeParseException ex) {
                            showAlert(Alert.AlertType.ERROR, "Invalid Date",
                                    "Expiry date must be in MM/DD/YYYY format.");
                            return null;
                        }
                    }
                    return m;
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input",
                            "Stock must be an integer and price a number.");
                }
            }
            return null;
        });

        Optional<Medicine> result = dlg.showAndWait();
        result.ifPresent(med -> {
            try {
                if (isNew) dao.add(med);
                else       dao.update(med);
                loadData();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Save Error", e.getMessage());
            }
        });
    }

    private void deleteItem(Medicine m) {
        if (!SessionManager.getInstance().getCurrentUser().getRole().canEditInventory()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Your role does not allow deleting medicines.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + m.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().filter(bt -> bt == ButtonType.YES).ifPresent(bt -> {
            try {
                dao.delete(m.getId(), m.getName());
                loadData();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Delete Error", e.getMessage());
            }
        });
    }

    @FXML
    private void printStockReport() {
        try {
            List<Medicine> list = dao.getAll();
            String path = ReportGenerator.generateStockReport(list);
            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(path)); }
            catch (Exception ignore) {}
            showAlert(Alert.AlertType.INFORMATION, "Report Generated", "Stock report saved to:\n" + path);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Report Error", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
