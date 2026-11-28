package com.blms.app;

import com.blms.ui.navigation.SceneManager;
import com.blms.ui.navigation.View;
import com.blms.ui.util.AlertHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/** OWNER: Ismail. JavaFX entry point. Bootstraps the BL/DB layers once, then shows the login screen. */
public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /** Set when the BL/DB layers could not start (e.g. Oracle is not running) - shown instead of a crash. */
    private RuntimeException startupError;

    @Override
    public void init() {
        try {
            AppContext.bootstrap();
        } catch (RuntimeException e) {          // UC01 extension "Database connection failure"
            startupError = e;
        }
    }

    @Override
    public void start(Stage stage) {
        if (startupError != null) {
            AlertHelper.error(startupError);    // DataAccessException: "Unable to connect to database"
            Platform.exit();
            return;
        }
        SceneManager.init(stage);
        SceneManager.switchTo(View.LOGIN);
        stage.show();
    }

    @Override
    public void stop() {
        if (startupError == null) {
            AppContext.get().shutdown();
        }
    }
}
