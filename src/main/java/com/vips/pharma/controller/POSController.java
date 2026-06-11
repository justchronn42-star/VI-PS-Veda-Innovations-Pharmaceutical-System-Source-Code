package com.vips.pharma.controller;

import com.vips.pharma.dao.InventoryDAO;
import com.vips.pharma.dao.ReceiptDAO;
import com.vips.pharma.model.CartItem;
import com.vips.pharma.model.Medicine;
import com.vips.pharma.model.Receipt;
import com.vips.pharma.report.ReportGenerator;
import com.vips.pharma.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class POSController implements Initializable {

    @FXML private TextField tfSearch;
    @FXML private ListView<Medicine> medicineList;
    @FXML private Label lblSelected, lblPrice, lblStock, lblSubtotal, lblTotal;
    @FXML private Spinner<Integer> spinQty;
    @FXML private Button btnAddToCart;

    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> colName;
    @FXML private TableColumn<CartItem, Integer> colQty;
    @FXML private TableColumn<CartItem, Double> colPrice;
    @FXML private TableColumn<CartItem, Double> colTotal;

    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final ReceiptDAO receiptDAO = new ReceiptDAO();

    private ObservableList<Medicine> allMedicines = FXCollections.observableArrayList();
    private FilteredList<Medicine> filtered;
    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private Receipt lastReceipt;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        spinQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));

        // Bind cart table columns
        colName.setCellValueFactory(new PropertyValueFactory<>("medName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        cartTable.setItems(cartItems);

        // Add right-click remove context menu
        cartTable.setRowFactory(tv -> {
            TableRow<CartItem> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem removeItem = new MenuItem("Remove Item");
            removeItem.setOnAction(e -> {
                CartItem item = row.getItem();
                if (item != null) {
                    cartItems.remove(item);
                    recalculateCartTotal();
                }
            });
            menu.getItems().add(removeItem);
            row.setContextMenu(menu);
            return row;
        });

        loadMedicines();

        filtered = new FilteredList<>(allMedicines, m -> true);
        tfSearch.textProperty().addListener((obs, o, n) ->
                filtered.setPredicate(m ->
                        n == null || n.isBlank() ||
                        m.getName().toLowerCase().contains(n.toLowerCase())));
        medicineList.setItems(filtered);

        // Visual: strike-through + grey for expired medicines
        medicineList.setCellFactory(lv -> new javafx.scene.control.ListCell<Medicine>() {
            @Override protected void updateItem(Medicine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if (item.isExpired()) {
                    setText(item.getName() + "  [EXPIRED]");
                    setStyle("-fx-text-fill: #aaa; -fx-strikethrough: true;");
                } else {
                    setText(item.getName());
                    setStyle("");
                }
            }
        });

        medicineList.getSelectionModel().selectedItemProperty().addListener(
                (obs, o, n) -> updateSelection(n));

        spinQty.valueProperty().addListener((obs, o, n) -> recalculate());
    }

    private void loadMedicines() {
        try {
            allMedicines.setAll(inventoryDAO.getAll());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
        }
    }

    private void updateSelection(Medicine m) {
        if (m == null) {
            lblSelected.setText("—");
            lblPrice.setText("₱0.00");
            lblStock.setText("0");
        } else {
            lblSelected.setText(m.getName());
            lblPrice.setText(String.format("₱%.2f", m.getPrice()));
            lblStock.setText(String.valueOf(m.getStock()));
            int currentStock = m.getStock();
            spinQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, currentStock), 1));
        }
        recalculate();
    }

    private void recalculate() {
        Medicine m = medicineList.getSelectionModel().getSelectedItem();
        if (m == null || spinQty.getValueFactory() == null || spinQty.getValue() == null) {
            lblSubtotal.setText("₱0.00");
            return;
        }
        int qty = spinQty.getValue();
        double amt = qty * m.getPrice();
        lblSubtotal.setText(String.format("₱%.2f", amt));
    }

    private void recalculateCartTotal() {
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
        lblTotal.setText(String.format("TOTAL: ₱%.2f", total));
    }

    @FXML
    private void handleAddToCart() {
        Medicine m = medicineList.getSelectionModel().getSelectedItem();
        if (m == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a medicine from the list first.");
            return;
        }

        if (m.isExpired()) {
            showAlert(Alert.AlertType.ERROR, "Expired Medicine",
                    m.getName() + " is expired and cannot be sold.");
            return;
        }

        int qty = spinQty.getValue();

        // Check stock including already-carted quantity
        int alreadyInCart = cartItems.stream()
                .filter(ci -> ci.getMedId() == m.getId())
                .mapToInt(CartItem::getQuantity)
                .sum();

        if (alreadyInCart + qty > m.getStock()) {
            showAlert(Alert.AlertType.WARNING, "Insufficient Stock",
                    "Only " + (m.getStock() - alreadyInCart) + " more units available for " + m.getName() + ".");
            return;
        }

        // Merge if same medicine already in cart
        boolean found = false;
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem existing = cartItems.get(i);
            if (existing.getMedId() == m.getId()) {
                cartItems.set(i, new CartItem(m, existing.getQuantity() + qty));
                found = true;
                break;
            }
        }

        if (!found) {
            cartItems.add(new CartItem(m, qty));
        }

        recalculateCartTotal();
        cartTable.refresh();

        // Reset spinner
        spinQty.getValueFactory().setValue(1);
    }

    @FXML
    private void processSale() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cart Empty", "Please add items to the cart before processing.");
            return;
        }

        double grandTotal = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
        int totalQty = cartItems.stream().mapToInt(CartItem::getQuantity).sum();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Process " + cartItems.size() + " item(s)\nGrand Total: ₱" + String.format("%.2f", grandTotal) + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Sale");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    // Deduct stock for each item
                    List<CartItem> soldItems = new ArrayList<>(cartItems);
                    for (CartItem ci : soldItems) {
                        inventoryDAO.deductStock(ci.getMedId(), ci.getQuantity());
                    }

                    // Build receipt item summary
                    StringBuilder names = new StringBuilder();
                    for (int i = 0; i < soldItems.size(); i++) {
                        if (i > 0) names.append(", ");
                        names.append(soldItems.get(i).getMedName())
                             .append(" x").append(soldItems.get(i).getQuantity());
                    }

                    String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .format(LocalDateTime.now());
                    Receipt r = new Receipt(0, names.toString(), totalQty, grandTotal, timestamp);
                    receiptDAO.save(r);
                    receiptDAO.saveItems(r.getId(), soldItems);
                    lastReceipt = r;

                    // Generate multi-item receipt PDF
                    String path = ReportGenerator.generateReceipt(r, soldItems);
                    openPDF(path);

                    loadMedicines();
                    cartItems.clear();
                    clearForm();

                    showAlert(Alert.AlertType.INFORMATION, "Sale Complete",
                            "Receipt saved to: " + path);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Sale Error", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void printLastReceipt() {
        if (lastReceipt == null) {
            showAlert(Alert.AlertType.WARNING, "No Receipt", "No sale has been made in this session.");
            return;
        }
        try {
            String path = ReportGenerator.generateReceiptReprint(lastReceipt);
            openPDF(path);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Print Error", e.getMessage());
        }
    }

    @FXML
    private void clearCart() {
        cartItems.clear();
        recalculateCartTotal();
    }

    @FXML
    private void clearForm() {
        medicineList.getSelectionModel().clearSelection();
        tfSearch.clear();
        if (spinQty.getValueFactory() != null) spinQty.getValueFactory().setValue(1);
        lblSelected.setText("—");
        lblPrice.setText("₱0.00");
        lblStock.setText("0");
        lblSubtotal.setText("₱0.00");
        cartItems.clear();
        lblTotal.setText("TOTAL: ₱0.00");
    }

    private void openPDF(String path) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(path));
            }
        } catch (Exception e) {
            System.out.println("Cannot auto-open PDF: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(null);
            a.showAndWait();
        });
    }
}
