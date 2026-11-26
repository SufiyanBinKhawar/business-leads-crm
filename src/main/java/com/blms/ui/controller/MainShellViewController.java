package com.blms.ui.controller;

import com.blms.domain.UserRole;
import com.blms.dto.LoginResult;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.navigation.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/** OWNER: Ismail. Sidebar + content area (UC01 step 4 "loads the Pipeline main dashboard"). */
public class MainShellViewController {

    @FXML private StackPane contentArea;
    @FXML private Label userLabel;
    @FXML private Button workspaceSettingsButton;

    @FXML
    private void initialize() {
        SceneManager.registerContentArea(contentArea);
    }

    public void setLoginResult(LoginResult result) {
        userLabel.setText(result.username() + " (" + result.role() + ")");
        boolean admin = result.role() == UserRole.ADMIN;       // UC02 precondition: setup privileges
        workspaceSettingsButton.setVisible(admin);
        workspaceSettingsButton.setManaged(admin);
        onDashboard();
    }

    @FXML private void onDashboard() { SceneManager.showInShell(View.DASHBOARD); }
    @FXML private void onLeads() { SceneManager.showInShell(View.LEADS); }
    @FXML private void onImportCsv() { SceneManager.showInShell(View.CSV_IMPORT); }
    @FXML private void onExport() { SceneManager.showInShell(View.EXPORT); }
    @FXML private void onWorkspaceSettings() { SceneManager.showInShell(View.WORKSPACE_SETTINGS); }

    @FXML
    private void onLogout() {
        SceneManager.switchTo(View.LOGIN);
    }
}
