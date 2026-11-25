package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.WorkspaceController;
import javafx.fxml.FXML;

/** OWNER: Ismail. UC02 Configure Company Workspace. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class WorkspaceSettingsViewController {

    @FXML private javafx.scene.control.TextField companyNameField;
    @FXML private javafx.scene.control.TextField targetIndustryField;
    @FXML private javafx.scene.control.TextArea servicesArea;
    @FXML private javafx.scene.control.TextField smtpHostField;
    @FXML private javafx.scene.control.TextField smtpPortField;
    @FXML private javafx.scene.control.TextField smtpUsernameField;
    @FXML private javafx.scene.control.PasswordField smtpPasswordField;

    private WorkspaceController workspaceController;

    @FXML
    private void initialize() {
        workspaceController = AppContext.get().workspaceController();
        // TODO(Ismail): SSD openWorkspaceSettings() -> WorkspaceForm -> fill fields (password stays empty)
    }

    /** UC02 SSD saveWorkspace(company, industry, services, new SmtpCredentials(host, port, user, password)); port must parse as int (else add 'field-error'); ValidationException -> add 'field-error' to empty fields; success -> info 'Workspace Configured Successfully' */
    @FXML
    private void onSave() {
        // TODO(Ismail)
    }
}
