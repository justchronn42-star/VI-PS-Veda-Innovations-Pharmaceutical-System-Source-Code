package com.vips.pharma.controller;

import com.vips.pharma.dao.UserDAO;
import com.vips.pharma.model.User;
import com.vips.pharma.util.LogoManager;
import com.vips.pharma.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField     tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Button        btnLogin;
    @FXML private Label         lblError;
    @FXML private Label         lblLogoEmoji;   // the 💊 label — hidden when custom logo loads
    @FXML private VBox          loginHeader;    // right-panel header VBox
    @FXML private StackPane     leftPanel;      // left branding panel
    @FXML private ImageView     ivBgImage;      // background image in left panel

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setVisible(false);
        pfPassword.setOnAction(e -> handleLogin());
        tfUsername.setOnAction(e -> pfPassword.requestFocus());

        // --- Swap emoji for custom logo (right-panel header) ---
        Image logo = LogoManager.loadLogo(72);
        if (logo != null && loginHeader != null && lblLogoEmoji != null) {
            int emojiIndex = loginHeader.getChildren().indexOf(lblLogoEmoji);
            if (emojiIndex >= 0) {
                ImageView iv = new ImageView(logo);
                iv.setFitWidth(72);
                iv.setFitHeight(72);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(255,219,77,0.55), 14, 0, 0, 0);");
                loginHeader.getChildren().set(emojiIndex, iv);
            }
        }

        // --- Load background image for right panel ---
        Image bgImage = loadLoginBackground();
        if (bgImage != null && ivBgImage != null && leftPanel != null) {
            ivBgImage.setImage(bgImage);
            // Bind ImageView dimensions to the StackPane so it always fills
            ivBgImage.fitWidthProperty().bind(leftPanel.widthProperty());
            ivBgImage.fitHeightProperty().bind(leftPanel.heightProperty());
        } else if (leftPanel != null && ivBgImage != null) {
            // No image — remove placeholder so CSS gradient shows
            leftPanel.getChildren().remove(ivBgImage);
        }
    }

    /**
     * Looks for login_bg.png or login_bg.jpg in the same directory as the
     * custom logo (app home or resources/images). Returns null if not found.
     */
    private Image loadLoginBackground() {
        String[] names = {"login_bg.png", "login_bg.jpg", "login_bg.jpeg"};
        String[] dirs  = {
            System.getProperty("user.home") + "/.vips_pharma/",
            System.getProperty("user.dir") + "/",
            System.getProperty("user.dir") + "/images/"
        };
        for (String dir : dirs) {
            for (String name : names) {
                File f = new File(dir + name);
                if (f.exists()) {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        return new Image(fis);
                    } catch (Exception ignored) {}
                }
            }
        }
        // Also try classpath
        try {
            URL cp = getClass().getResource("/images/login_bg.png");
            if (cp != null) return new Image(cp.toExternalForm());
            cp = getClass().getResource("/images/login_bg.jpg");
            if (cp != null) return new Image(cp.toExternalForm());
        } catch (Exception ignored) {}
        return null;
    }

    @FXML
    private void handleLogin() {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                showError("Invalid credentials or account is inactive.");
                pfPassword.clear();
                return;
            }
            SessionManager.getInstance().login(user);
            openMainWindow();
        } catch (Exception ex) {
            showError("Database error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void openMainWindow() {
        Stage loginStage = (Stage) tfUsername.getScene().getWindow();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
            Stage mainStage = new Stage();
            double w = Screen.getPrimary().getVisualBounds().getWidth();
            double h = Screen.getPrimary().getVisualBounds().getHeight();
            Scene scene = new Scene(root, w, h);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            mainStage.setTitle("VI-PS Pharma POS & Inventory  —  "
                    + SessionManager.getInstance().getCurrentUser().getUsername()
                    + " (" + SessionManager.getInstance().getCurrentUser().getRole().displayName() + ")");
            mainStage.setMinWidth(900);
            mainStage.setMinHeight(600);
            mainStage.setMaximized(true);
            mainStage.setScene(scene);

            // Carry the same window icon to the main window
            Image iconRaw = LogoManager.loadLogoRaw();
            if (iconRaw != null) mainStage.getIcons().add(iconRaw);

            mainStage.show();
            Platform.runLater(loginStage::close);
        } catch (Exception e) {
            showError("Failed to open main window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExit() { Platform.exit(); }

    private void showError(String msg) {
        lblError.setText("⚠ " + msg);
        lblError.setVisible(true);
    }
}
