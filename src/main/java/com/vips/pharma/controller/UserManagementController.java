package com.vips.pharma.controller;

import com.vips.pharma.dao.UserDAO;
import com.vips.pharma.model.Role;
import com.vips.pharma.model.User;
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

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * User Management screen – accessible by ADMIN only.
 * Provides CRUD for the users table.
 */
public class UserManagementController implements Initializable {

    @FXML private TableView<User>            userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername, colRole, colStatus, colActions;
    @FXML private Label                      lblCount;

    private final UserDAO dao = new UserDAO();
    private final ObservableList<User> items = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));

        colRole.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getRole().displayName()));

        colStatus.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().isActive() ? "✔ Active" : "✖ Inactive"));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.startsWith("✔")
                        ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                        : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });

        // Actions column
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏ Edit");
            private final Button btnDelete = new Button("🗑 Delete");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-sm-primary");
                btnDelete.getStyleClass().add("btn-sm-danger");
                btnEdit.setOnAction(e   -> showEditDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        userTable.setItems(items);
        loadData();
    }

    private void loadData() {
        try {
            items.setAll(dao.getAll());
            lblCount.setText("Total Users: " + items.size());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
        }
    }

    // ── Add ──────────────────────────────────────────────────────────────

    @FXML
    private void showAddDialog() {
        Dialog<Void> dlg = buildDialog("Add New User", null);
        dlg.showAndWait();
        loadData();
    }

    // ── Edit ─────────────────────────────────────────────────────────────

    private void showEditDialog(User user) {
        // Prevent editing own account role/status (safety guard)
        User me = SessionManager.getInstance().getCurrentUser();
        if (user.getId() == me.getId()) {
            showAlert(Alert.AlertType.WARNING, "Not Allowed",
                    "You cannot edit your own account here. Contact another admin.");
            return;
        }
        Dialog<Void> dlg = buildDialog("Edit User", user);
        dlg.showAndWait();
        loadData();
    }

    // ── Delete ────────────────────────────────────────────────────────────

    private void deleteUser(User user) {
        User me = SessionManager.getInstance().getCurrentUser();
        if (user.getId() == me.getId()) {
            showAlert(Alert.AlertType.WARNING, "Not Allowed",
                    "You cannot delete your own account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user \"" + user.getUsername() + "\"?\n"
                + "This action cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                dao.delete(user);
                loadData();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Delete Error", e.getMessage());
            }
        });
    }

    // ── Shared dialog builder ─────────────────────────────────────────────

    /**
     * Builds the add/edit dialog.
     *
     * @param title the dialog header
     * @param user  existing user to edit, or {@code null} for a new one
     */
    private Dialog<Void> buildDialog(String title, User user) {
        boolean isNew = (user == null);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField     tfUsername = new TextField(isNew ? "" : user.getUsername());
        PasswordField pfPassword = new PasswordField();
        PasswordField pfConfirm  = new PasswordField();
        ComboBox<Role> cbRole    = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        CheckBox       cbActive  = new CheckBox("Active");

        tfUsername.setPromptText("Username");
        pfPassword.setPromptText(isNew ? "Password" : "New password (leave blank to keep)");
        pfConfirm.setPromptText("Confirm password");
        cbRole.getSelectionModel().select(isNew ? Role.CASHIER : user.getRole());
        cbActive.setSelected(isNew || user.isActive());

        // Display role names
        cbRole.setButtonCell(roleCell());
        cbRole.setCellFactory(lv -> roleCell());

        VBox form = new VBox(10,
                new Label("Username:"), tfUsername,
                new Label("Password" + (isNew ? ":" : " (blank = no change):")), pfPassword,
                new Label("Confirm Password:"), pfConfirm,
                new Label("Role:"), cbRole,
                cbActive);
        form.setPadding(new Insets(16));
        form.setPrefWidth(320);
        dlg.getDialogPane().setContent(form);

        // Validation + save on OK
        dlg.getDialogPane().lookupButton(saveBtn).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
            String uname = tfUsername.getText().trim();
            String pass  = pfPassword.getText();
            String conf  = pfConfirm.getText();

            if (uname.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Username cannot be empty.");
                event.consume(); return;
            }
            if (isNew && pass.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Password is required for new users.");
                event.consume(); return;
            }
            if (!pass.isEmpty() && !pass.equals(conf)) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Passwords do not match.");
                event.consume(); return;
            }
            if (cbRole.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Please select a role.");
                event.consume(); return;
            }

            try {
                if (isNew) {
                    dao.add(uname, pass, cbRole.getValue());
                } else {
                    user.setUsername(uname);
                    user.setRole(cbRole.getValue());
                    user.setActive(cbActive.isSelected());
                    dao.update(user, pass.isEmpty() ? null : pass);
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Save Error", ex.getMessage());
                event.consume();
            }
        });

        return dlg;
    }

    private ListCell<Role> roleCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Role r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.displayName());
            }
        };
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
