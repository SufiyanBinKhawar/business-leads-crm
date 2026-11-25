package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadController;
import com.blms.domain.Lead;
import javafx.fxml.FXML;

/** OWNER: Ismail. UC03 Manually Add New Lead (add mode) and UC06 edit/archive (edit mode). Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class LeadFormViewController {

    @FXML private javafx.scene.control.TextField companyNameField;
    @FXML private javafx.scene.control.TextField contactNameField;
    @FXML private javafx.scene.control.TextField emailField;
    @FXML private javafx.scene.control.TextField jobRoleField;
    @FXML private javafx.scene.control.TextField industryField;   // edit mode
    @FXML private javafx.scene.control.TextField phoneField;      // edit mode
    @FXML private javafx.scene.control.CheckBox archivedCheck;    // edit mode
    @FXML private javafx.scene.control.Label errorLabel;

    private LeadController leadController;
    private Long editingLeadId; // null = add mode

    @FXML
    private void initialize() {
        leadController = AppContext.get().leadController();
        errorLabel.setText("");
    }

    /** Edit mode (UC06): called with leadController.editLead(id) before the dialog is shown. */
    public void openForEdit(Lead lead) {
        this.editingLeadId = lead.getLeadId();
        // TODO(Ismail): fill fields from lead getters / lead.getPrimaryContact(); show industry/phone/archived
    }

    /** add: SSD saveLead(company, contact, email, role) (DuplicateLeadException -> warn; ValidationException -> errorLabel). edit: SSD saveLeadChanges(editingLeadId, new LeadData(..., null), archivedCheck.isSelected()) */
    @FXML
    private void onSave() {
        // TODO(Ismail)
    }

    /** close the dialog stage */
    @FXML
    private void onCancel() {
        // TODO(Ismail)
    }
}
