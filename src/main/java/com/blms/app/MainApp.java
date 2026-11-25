package com.blms.app;

import com.blms.ui.navigation.SceneManager;
import com.blms.ui.navigation.View;
import javafx.application.Application;
import javafx.stage.Stage;

/** OWNER: Ismail. JavaFX entry point. Bootstraps the BL/DB layers once, then shows the login screen. */
public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        AppContext.bootstrap();
    }

    @Override
    public void start(Stage stage) {
        SceneManager.init(stage);
        SceneManager.switchTo(View.LOGIN);
        stage.show();
    }

    @Override
    public void stop() {
        AppContext.get().shutdown();
    }
}
