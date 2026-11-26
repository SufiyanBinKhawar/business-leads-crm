package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadController;
import com.blms.domain.Contact;
import com.blms.domain.Lead;
import com.blms.dto.LeadData;
import com.blms.dto.LeadForm;
import com.blms.exception.BlmsException;
import com.blms.exception.DuplicateLeadException;
import com.blms.exception.ValidationException;
import com.blms.ui.util.AlertHelper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * OWNER: Ismail. Lead Form Dialog.
 * Used for both Add Mode (UC03) and Edit/Archive Mode (UC06).
 * Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only.
 */
public class LeadFormViewController {

    @FXML private Label formTitleLabel;
    @FXML private Label formSubtitleLabel;

    @FXML private TextField companyNameField;
    @FXML private VBox industryBox;
    @FXML private TextField industryField;

    @FXML private TextField contactNameField;
    @FXML private TextField emailField;
    @FXML private TextField jobRoleField;
    @FXML private VBox phoneBox;
    @FXML private TextField phoneField;

    @FXML private VBox archivedBox;
    @FXML private CheckBox archivedCheck;

    @FXML private Label errorLabel;
    @FXML private ProgressIndicator saveProgress;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private LeadController leadController;
    private Long editingLeadId; // null = Add mode, non-null = Edit mode
    private Runnable onSuccessCallback;

    @FXML
    private void initialize() {
        leadController = AppContext.get().leadController();
        errorLabel.setText("");
        if (saveProgress != null) {
            saveProgress.setVisible(false);
        }
    }

    /** ADD Mode (UC03): call requestNewLeadForm() to initialize blank defaults. */
    public void openForAdd(Runnable onSuccess) {
        this.editingLeadId = null;
        this.onSuccessCallback = onSuccess;

        formTitleLabel.setText("Add New Lead");
        formSubtitleLabel.setText("Enter prospect company and primary contact details (UC03)");
        archivedBox.setVisible(false);
        archivedBox.setManaged(false);
        // D5 saveLead(companyName, contactName, email, jobRole): industry and phone are edited later (UC06)
        industryBox.setVisible(false);
        industryBox.setManaged(false);
        phoneBox.setVisible(false);
        phoneBox.setManaged(false);

        try {
            LeadForm blankForm = leadController.requestNewLeadForm();
            companyNameField.setText(blankForm.companyName());
            contactNameField.setText(blankForm.contactName());
            emailField.setText(blankForm.email());
            jobRoleField.setText(blankForm.jobRole());
        } catch (Exception e) {
            companyNameField.clear();
            contactNameField.clear();
            emailField.clear();
            jobRoleField.clear();
        }
        industryField.clear();
        phoneField.clear();
        archivedCheck.setSelected(false);
    }

    /** EDIT Mode (UC06): call editLead(leadId) and prefill fields. */
    public void openForEdit(Long leadId, Runnable onSuccess) {
        this.editingLeadId = leadId;
        this.onSuccessCallback = onSuccess;

        formTitleLabel.setText("Edit Lead");
        formSubtitleLabel.setText("Update lead details or change pipeline archive status (UC06)");
        archivedBox.setVisible(true);
        archivedBox.setManaged(true);

        try {
            Lead lead = leadController.editLead(leadId);
            if (lead != null) {
                populateFields(lead);
            }
        } catch (BlmsException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    /** Overload for direct Lead prefill. */
    public void openForEdit(Lead lead) {
        openForEdit(lead, null);
    }

    public void openForEdit(Lead lead, Runnable onSuccess) {
        this.editingLeadId = lead != null ? lead.getLeadId() : null;
        this.onSuccessCallback = onSuccess;

        formTitleLabel.setText("Edit Lead");
        formSubtitleLabel.setText("Update lead details or change pipeline archive status (UC06)");
        archivedBox.setVisible(true);
        archivedBox.setManaged(true);

        if (lead != null) {
            populateFields(lead);
        }
    }

    private void populateFields(Lead lead) {
        companyNameField.setText(lead.getCompanyName());
        industryField.setText(lead.getIndustry() != null ? lead.getIndustry() : "");
        phoneField.setText(lead.getPhone() != null ? lead.getPhone() : "");
        archivedCheck.setSelected(lead.isArchived());

        if (!lead.getContacts().isEmpty()) {
            Contact contact = lead.getPrimaryContact();
            if (contact != null) {
                contactNameField.setText(contact.getFullName() != null ? contact.getFullName() : "");
                emailField.setText(contact.getEmail() != null ? contact.getEmail() : "");
                jobRoleField.setText(contact.getJobRole() != null ? contact.getJobRole() : "");
            }
        }
    }

    /** Client-side validation followed by asynchronous controller dispatch. */
    @FXML
    private void onSave() {
        clearFieldErrors();
        errorLabel.setText("");

        String company = companyNameField.getText() != null ? companyNameField.getText().trim() : "";
        String contact = contactNameField.getText() != null ? contactNameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String role = jobRoleField.getText() != null ? jobRoleField.getText().trim() : "";
        String industry = industryField.getText() != null ? industryField.getText().trim() : "";
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";

        // Client-side validation: required fields
        if (company.isEmpty()) {
            companyNameField.getStyleClass().add("field-error");
            errorLabel.setText("Company name is required.");
            companyNameField.requestFocus();
            return;
        }

        if (contact.isEmpty()) {
            contactNameField.getStyleClass().add("field-error");
            errorLabel.setText("Primary contact name is required.");
            contactNameField.requestFocus();
            return;
        }

        // Client-side validation: email format
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            emailField.getStyleClass().add("field-error");
            errorLabel.setText("A valid email address is required (e.g. name@company.com).");
            emailField.requestFocus();
            return;
        }

        saveButton.setDisable(true);
        if (saveProgress != null) {
            saveProgress.setVisible(true);
        }

        boolean isEdit = editingLeadId != null;
        boolean archived = archivedCheck.isSelected();

        Service<Void> service = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        if (!isEdit) {
                            // UC03: SSD saveLead(companyName, contactName, email, jobRole)
                            leadController.saveLead(company, contact, email, role);
                        } else {
                            // UC06: SSD saveLeadChanges(leadId, updates, archived)
                            LeadData updates = new LeadData(company, contact, email, role, industry, phone, null);
                            leadController.saveLeadChanges(editingLeadId, updates, archived);
                        }
                        return null;
                    }
                };
            }
        };

        service.setOnSucceeded(e -> {
            saveButton.setDisable(false);
            if (saveProgress != null) {
                saveProgress.setVisible(false);
            }
            closeDialog();
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
        });

        service.setOnFailed(e -> {
            saveButton.setDisable(false);
            if (saveProgress != null) {
                saveProgress.setVisible(false);
            }

            Throwable error = service.getException();
            if (error instanceof DuplicateLeadException || (error != null && error.getCause() instanceof DuplicateLeadException)) {
                String msg = error instanceof DuplicateLeadException ? error.getMessage() : error.getCause().getMessage();
                emailField.getStyleClass().add("field-error");
                errorLabel.setText(msg);
                offerExistingProfile(msg, email);        // D3 UC03: "offers a link to go to the existing profile"
            } else if (error instanceof ValidationException || (error != null && error.getCause() instanceof ValidationException)) {
                String msg = error instanceof ValidationException ? error.getMessage() : error.getCause().getMessage();
                errorLabel.setText(msg);
            } else if (error instanceof BlmsException) {
                errorLabel.setText(error.getMessage());
            } else {
                String msg = error != null && error.getMessage() != null ? error.getMessage() : "Failed to save lead";
                errorLabel.setText(msg);
            }
        });

        service.start();
    }

    private void clearFieldErrors() {
        companyNameField.getStyleClass().remove("field-error");
        contactNameField.getStyleClass().remove("field-error");
        emailField.getStyleClass().remove("field-error");
        jobRoleField.getStyleClass().remove("field-error");
        industryField.getStyleClass().remove("field-error");
        phoneField.getStyleClass().remove("field-error");
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    /** UC03 duplicate extension: find the lead with the same email domain (SSD filterLeads) and open its profile. */
    private void offerExistingProfile(String message, String email) {
        if (!AlertHelper.confirm("Duplicate Lead", message + "\n\nOpen the existing lead's profile?")) {
            return;
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        try {
            java.util.List<Lead> matches = leadController.filterLeads(
                    new com.blms.dto.LeadFilter(null, null, "@" + domain, true));
            if (matches.isEmpty()) {
                AlertHelper.warn("Duplicate Lead", "The existing lead could not be found.");
                return;
            }
            closeDialog();
            LeadDetailViewController detail = com.blms.ui.navigation.SceneManager.showInShell(
                    com.blms.ui.navigation.View.LEAD_DETAIL);
            detail.setLead(matches.get(0).getLeadId());
        } catch (BlmsException e) {
            AlertHelper.error(e);
        }
    }
}
