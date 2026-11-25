package com.blms.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/** OWNER: Ismail. Central navigation. View controllers NEVER create Stages or load FXML themselves. */
public final class SceneManager {

    private static final String CSS = "/com/blms/ui/css/app.css";
    private static Stage primaryStage;
    private static StackPane contentArea;

    private SceneManager() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
    }

    /** Replaces the whole window (LOGIN <-> MAIN_SHELL). */
    public static <C> C switchTo(View view) {
        FXMLLoader loader = loader(view);
        Parent root = load(loader);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(SceneManager.class.getResource(CSS)).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle(view.getTitle());
        return loader.getController();
    }

    /** Called once by MainShellViewController.initialize() to register its content area. */
    public static void registerContentArea(StackPane area) {
        contentArea = area;
    }

    /** Swaps the centre of the main shell (sidebar navigation). */
    public static <C> C showInShell(View view) {
        FXMLLoader loader = loader(view);
        contentArea.getChildren().setAll(load(loader));
        primaryStage.setTitle("Business Leads CRM - " + view.getTitle());
        return loader.getController();
    }

    /** Opens a modal dialog and returns its controller BEFORE showing, so the caller can pass data in. */
    public static <C> DialogHandle<C> openDialog(View view) {
        FXMLLoader loader = loader(view);
        Parent root = load(loader);
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(view.getTitle());
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(SceneManager.class.getResource(CSS)).toExternalForm());
        dialog.setScene(scene);
        return new DialogHandle<>(dialog, loader.getController());
    }

    public static Stage primaryStage() {
        return primaryStage;
    }

    private static FXMLLoader loader(View view) {
        return new FXMLLoader(Objects.requireNonNull(SceneManager.class.getResource(view.getFxmlPath()),
                "Missing FXML " + view.getFxmlPath()));
    }

    private static Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + loader.getLocation(), e);
        }
    }

    /** Dialog stage + its controller. Call {@code stage().showAndWait()} after passing data in. */
    public record DialogHandle<C>(Stage stage, C controller) {
    }
}
