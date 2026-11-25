package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.WorkspaceController;
import com.blms.dto.SmtpCredentials;
import com.blms.dto.WorkspaceForm;
import com.blms.exception.BlmsException;
import com.blms.exception.ValidationException;
import com.blms.ui.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** OWNER: Ismail. UC02 Configure Company Workspace. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class WorkspaceSettingsViewController {

    @FXML private TextField companyNameField;
    @FXML private TextField targetIndustryField;
    @FXML private TextArea servicesArea;
    @FXML private TextField smtpHostField;
    @FXML private TextField smtpPortField;
    @FXML private TextField smtpUsernameField;
    @FXML private PasswordField smtpPasswordField;

    private WorkspaceController workspaceController;

    @FXML
    private void initialize() {
        workspaceController = AppContext.get().workspaceController();
        try {
            WorkspaceForm form = workspaceController.openWorkspaceSettings();
            if (form != null) {
                companyNameField.setText(form.companyName());
                targetIndustryField.setText(form.targetIndustry());
                servicesArea.setText(form.servicesOffered());
                smtpHostField.setText(form.smtpHost());
                smtpPortField.setText(form.smtpPort() > 0 ? String.valueOf(form.smtpPort()) : "");
                smtpUsernameField.setText(form.smtpUsername());
            }
        } catch (UnsupportedOperationException e) {
            // Placeholder: Backend UC02 openWorkspaceSettings is not implemented yet
        }
    }

    /** UC02 SSD saveWorkspace(...). Port must parse as int; ValidationException -> field-error; success -> info. */
    @FXML
    private void onSave() {
        clearFieldErrors();
        String company = companyNameField.getText();
        String industry = targetIndustryField.getText();
        String services = servicesArea.getText();
        String host = smtpHostField.getText();
        String portStr = smtpPortField.getText();
        String user = smtpUsernameField.getText();
        String password = smtpPasswordField.getText();

        int port = 0;
        if (portStr != null && !portStr.isBlank()) {
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException e) {
                smtpPortField.getStyleClass().add("field-error");
                AlertHelper.warn("Validation Error", "SMTP Port must be a valid number (e.g. 587 or 465).");
                return;
            }
        }

        SmtpCredentials smtp = new SmtpCredentials(host, port, user, password);
        try {
            workspaceController.saveWorkspace(company, industry, services, smtp);
            AlertHelper.info("Workspace Settings", "Workspace configured successfully.");
        } catch (UnsupportedOperationException e) {
            AlertHelper.info("Workspace Settings (UC02)", "Workspace settings persistence is under active development by Sufiyan.");
        } catch (ValidationException e) {
            highlightErrors();
            AlertHelper.error(e);
        } catch (BlmsException e) {
            AlertHelper.error(e);
        }
    }

    private void clearFieldErrors() {
        companyNameField.getStyleClass().remove("field-error");
        targetIndustryField.getStyleClass().remove("field-error");
        servicesArea.getStyleClass().remove("field-error");
        smtpPortField.getStyleClass().remove("field-error");
    }

    private void highlightErrors() {
        if (companyNameField.getText() == null || companyNameField.getText().isBlank()) {
            companyNameField.getStyleClass().add("field-error");
        }
        if (targetIndustryField.getText() == null || targetIndustryField.getText().isBlank()) {
            targetIndustryField.getStyleClass().add("field-error");
        }
        if (servicesArea.getText() == null || servicesArea.getText().isBlank()) {
            servicesArea.getStyleClass().add("field-error");
        }
    }
}
