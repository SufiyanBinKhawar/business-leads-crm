package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadController;
import com.blms.controller.OutreachController;
import com.blms.domain.Lead;
import com.blms.domain.PipelineStatus;
import javafx.fxml.FXML;

/** OWNER: Ismail. UC06 Manage Existing Leads + batch actions for UC09 (generate drafts) and UC11 (send emails). Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class LeadsViewController {

    @FXML private javafx.scene.control.TableView<Lead> leadsTable;   // multi-select; columns read Lead getters only
    @FXML private javafx.scene.control.TextField searchField;
    @FXML private javafx.scene.control.TextField industryFilter;
    @FXML private javafx.scene.control.ComboBox<PipelineStatus> statusFilter;
    @FXML private javafx.scene.control.CheckBox showArchivedCheck;

    private LeadController leadController;
    private OutreachController outreachController;

    @FXML
    private void initialize() {
        leadController = AppContext.get().leadController();
        outreachController = AppContext.get().outreachController();
        // TODO(Ismail): columns (company, contact name/email via getPrimaryContact(), industry, status, createdAt); onApplyFilter()
    }

    /** UC06 SSD filterLeads(new LeadFilter(industry, status, search, showArchived)) -> leadsTable.setItems(...) */
    @FXML
    private void onApplyFilter() {
        // TODO(Ismail)
    }

    /** UC03 SSD requestNewLeadForm(): open View.LEAD_FORM dialog in add mode; after it closes -> onApplyFilter() */
    @FXML
    private void onAddLead() {
        // TODO(Ismail)
    }

    /** UC06 SSD editLead(leadId): open View.LEAD_FORM dialog, controller.openForEdit(lead) */
    @FXML
    private void onEditLead() {
        // TODO(Ismail)
    }

    /** UC07/UC08/UC10: SceneManager.showInShell(View.LEAD_DETAIL) then controller.setLead(leadId) */
    @FXML
    private void onOpenLead() {
        // TODO(Ismail)
    }

    /** UC04: open View.CSV_IMPORT dialog */
    @FXML
    private void onImportCsv() {
        // TODO(Ismail)
    }

    /** UC09 SSD generateDrafts(selected QUALIFIED ids) -> jobId; FxAsync.onComplete -> info(getLastResultCount() drafts ready, getLastFailureCount() failed) */
    @FXML
    private void onGenerateDrafts() {
        // TODO(Ismail)
    }

    /** UC11 SSD sendApprovedEmails(selected ids) -> jobId; FxAsync.onComplete -> info(getLastResultCount() sent); error -> AlertHelper.error */
    @FXML
    private void onSendEmails() {
        // TODO(Ismail)
    }
}
