package com.sofoste.thorcompanion;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public final class ThorApp extends Application {
    private static String screenshotPath;

    static void setScreenshotPath(String path) {
        screenshotPath = path;
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(ThorApp.class.getResource("thor-view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 780);
        scene.getStylesheets().add(ThorApp.class.getResource("thor.css").toExternalForm());

        stage.setTitle("THOR Companion · First Signal");
        stage.setMinWidth(1020);
        stage.setMinHeight(700);
        stage.setScene(scene);

        ThorController controller = loader.getController();
        if (screenshotPath != null) {
            controller.loadDemoData();
            stage.setResizable(false);
        }
        stage.show();

        if (screenshotPath != null) {
            Platform.runLater(() -> capture(scene, controller));
        }
    }

    private void capture(Scene scene, ThorController controller) {
        try {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            WritableImage image = new WritableImage((int) scene.getWidth(), (int) scene.getHeight());
            scene.snapshot(image);
            File target = new File(screenshotPath).getAbsoluteFile();
            if (target.getParentFile() != null) target.getParentFile().mkdirs();
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", target);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the THOR interface screenshot.", exception);
        } finally {
            controller.shutdown();
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        // The controller owns only daemon-like UI timers, so JavaFX can stop cleanly.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
