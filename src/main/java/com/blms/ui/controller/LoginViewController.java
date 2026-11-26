package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LoginController;
import com.blms.dto.LoginResult;
import com.blms.exception.AuthenticationException;
import com.blms.exception.BlmsException;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.navigation.View;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/**
 * OWNER: Ismail. UC01 screen: Authenticate System User.
 * Reference implementation for asynchronous controller calls from JavaFX.
 */
public class LoginViewController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loginProgress;

    private LoginController loginController;

    @FXML
    private void initialize() {
        loginController = AppContext.get().loginController();
        errorLabel.setText("");
        if (loginProgress != null) {
            loginProgress.setVisible(false);
        }
    }

    /** UC01 step 1 -> SSD login(username, password) running asynchronously. */
    @FXML
    private void onLogin() {
        if (loginButton.isDisabled()) {
            return;
        }

        String username = usernameField.getText();
        String password = passwordField.getText();

        errorLabel.setText("");
        loginButton.setDisable(true);
        if (loginProgress != null) {
            loginProgress.setVisible(true);
        }

        Service<LoginResult> loginService = new Service<>() {
            @Override
            protected Task<LoginResult> createTask() {
                return new Task<>() {
                    @Override
                    protected LoginResult call() {
                        return loginController.login(username, password);
                    }
                };
            }
        };

        loginService.setOnSucceeded(event -> {
            loginButton.setDisable(false);
            if (loginProgress != null) {
                loginProgress.setVisible(false);
            }
            LoginResult result = loginService.getValue();
            MainShellViewController shell = SceneManager.switchTo(View.MAIN_SHELL);
            shell.setLoginResult(result);
        });

        loginService.setOnFailed(event -> {
            loginButton.setDisable(false);
            if (loginProgress != null) {
                loginProgress.setVisible(false);
            }
            passwordField.clear();

            Throwable error = loginService.getException();
            if (error instanceof AuthenticationException || error instanceof BlmsException) {
                errorLabel.setText(error.getMessage());
            } else if (error != null && error.getCause() instanceof BlmsException) {
                errorLabel.setText(error.getCause().getMessage());
            } else {
                String msg = error != null && error.getMessage() != null ? error.getMessage() : "Authentication failed";
                errorLabel.setText(msg);
            }
            passwordField.requestFocus();
        });

        loginService.start();
    }
}
