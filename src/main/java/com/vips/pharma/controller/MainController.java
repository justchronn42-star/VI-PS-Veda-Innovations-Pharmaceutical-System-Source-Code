package com.vips.pharma.controller;

import com.vips.pharma.model.Role;
import com.vips.pharma.model.User;
import com.vips.pharma.util.LogoManager;
import com.vips.pharma.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label lblStatus;
    @FXML private Label lblUserInfo;

    @FXML private Button btnPOS;
    @FXML private Button btnInventory;
    @FXML private Button btnReceipts;
    @FXML private Button btnAudit;
    @FXML private Button btnUsers;
    @FXML private Button btnLogout;

    // The emoji label and its parent VBox in the sidebar logo section
    @FXML private Label lblSidebarLogoEmoji;
    @FXML private VBox  sidebarLogoBox;

    private Button activeBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        applyRolePermissions();
        swapSidebarLogo();

        User user = SessionManager.getInstance().getCurrentUser();
        Role role = user.getRole();
        if (role.canProcessSale())       showPOS();
        else if (role.canEditInventory()) showInventory();
        else                              showReceipts();
    }

    /** Replace the 💊 emoji with a custom logo ImageView if logo file exists. */
    private void swapSidebarLogo() {
        Image logo = LogoManager.loadLogo(48);
        if (logo != null && sidebarLogoBox != null && lblSidebarLogoEmoji != null) {
            int idx = sidebarLogoBox.getChildren().indexOf(lblSidebarLogoEmoji);
            if (idx >= 0) {
                ImageView iv = new ImageView(logo);
                iv.setFitWidth(48);
                iv.setFitHeight(48);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                iv.setStyle(
                    "-fx-effect: dropshadow(gaussian, rgba(255,219,77,0.5), 10, 0, 0, 0);");
                sidebarLogoBox.getChildren().set(idx, iv);
            }
        }
    }

    private void applyRolePermissions() {
        User user = SessionManager.getInstance().getCurrentUser();
        Role role = user.getRole();

        btnPOS.setVisible(role.canProcessSale());
        btnPOS.setManaged(role.canProcessSale());

        btnInventory.setVisible(role.canViewInventory());
        btnInventory.setManaged(role.canViewInventory());

        btnReceipts.setVisible(role.canViewReceipts());
        btnReceipts.setManaged(role.canViewReceipts());

        btnAudit.setVisible(role.canViewAudit());
        btnAudit.setManaged(role.canViewAudit());

        btnUsers.setVisible(role.canManageUsers());
        btnUsers.setManaged(role.canManageUsers());

        lblUserInfo.setText("👤 " + user.getUsername()
                + "\n" + role.displayName());
    }

    @FXML private void showPOS()       { load("/fxml/POSView.fxml",           btnPOS);       }
    @FXML private void showInventory() { load("/fxml/InventoryView.fxml",      btnInventory); }
    @FXML private void showReceipts()  { load("/fxml/ReceiptsView.fxml",       btnReceipts);  }
    @FXML private void showAudit()     { load("/fxml/AuditView.fxml",          btnAudit);     }
    @FXML private void showUsers()     { load("/fxml/UserManagementView.fxml", btnUsers);     }

    @FXML
    private void handleLogout() {
        Stage mainStage = (Stage) btnLogout.getScene().getWindow();
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
            double w = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
            double h = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
            Stage loginStage = new Stage();
            Scene scene = new Scene(root, w, h);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            loginStage.setTitle("VI-PS Pharma — Login");
            loginStage.setResizable(true);
            loginStage.setMaximized(true);
            // Apply window icon
            javafx.scene.image.Image icon = com.vips.pharma.util.LogoManager.loadLogoRaw();
            if (icon != null) loginStage.getIcons().add(icon);
            loginStage.setScene(scene);
            loginStage.show();
            Platform.runLater(mainStage::close);
        } catch (Exception e) {
            lblStatus.setText("Logout error: " + e.getMessage());
        }
    }

    private void load(String fxml, Button btn) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxml));
            contentArea.getChildren().setAll(view);
            if (activeBtn != null) activeBtn.getStyleClass().remove("nav-btn-active");
            btn.getStyleClass().add("nav-btn-active");
            activeBtn = btn;
            lblStatus.setText("✔ Ready");
        } catch (Exception e) {
            lblStatus.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
