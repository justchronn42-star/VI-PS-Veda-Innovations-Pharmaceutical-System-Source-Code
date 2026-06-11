package com.vips.pharma;

import com.vips.pharma.util.DatabaseUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Show login screen first
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));

        // Full screen dimensions
        double w = Screen.getPrimary().getVisualBounds().getWidth();
        double h = Screen.getPrimary().getVisualBounds().getHeight();

        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setTitle("VI-PS Pharma — Login");
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);

        // Set window icon to logo.png if available
        javafx.scene.image.Image icon = com.vips.pharma.util.LogoManager.loadLogoRaw();
        if (icon != null) primaryStage.getIcons().add(icon);

        primaryStage.show();
    }

    @Override
    public void stop() {
        DatabaseUtil.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
