package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LoginController;
import com.blms.dto.LoginResult;
import com.blms.exception.AuthenticationException;
import com.blms.exception.BlmsException;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.navigation.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * OWNER: Ismail. UC01 screen. REFERENCE IMPLEMENTATION for every view controller:
 * read fields -> call ONE D5 controller method -> render the result / show the error message.
 */
public class LoginViewController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private LoginController loginController;

    @FXML
    private void initialize() {
        loginController = AppContext.get().loginController();
        errorLabel.setText("");
    }

    /** UC01 step 1 -> SSD login(username, password). */
    @FXML
    private void onLogin() {
        try {
            LoginResult result = loginController.login(usernameField.getText(), passwordField.getText());
            MainShellViewController shell = SceneManager.switchTo(View.MAIN_SHELL);
            shell.setLoginResult(result);
        } catch (AuthenticationException e) {
            errorLabel.setText(e.getMessage());   // UC01 extension: show message ...
            passwordField.clear();                // ... and clear the password field
        } catch (BlmsException e) {
            errorLabel.setText(e.getMessage());   // e.g. ValidationException "Username is required"
        }
    }
}
