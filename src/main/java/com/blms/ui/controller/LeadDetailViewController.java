package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadController;
import com.blms.domain.EmailDraft;
import com.blms.domain.InteractionLog;
import com.blms.domain.PipelineStatus;
import javafx.fxml.FXML;

/** OWNER: Ismail. UC07 Log Manual Interaction, UC08 Update Pipeline Status, entry to UC10 drafts. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class LeadDetailViewController {

    @FXML private javafx.scene.control.Label companyLabel;
    @FXML private javafx.scene.control.ComboBox<PipelineStatus> statusCombo;
    @FXML private javafx.scene.control.ListView<InteractionLog> timelineList;
    @FXML private javafx.scene.control.ListView<EmailDraft> draftsList;
    @FXML private javafx.scene.control.TextArea noteArea;
    @FXML private javafx.scene.control.Button submitNoteButton;

    private LeadController leadController;
    private Long leadId;

    @FXML
    private void initialize() {
        leadController = AppContext.get().leadController();
        // UC07 extension: Submit disabled until text is entered
        submitNoteButton.disableProperty().bind(noteArea.textProperty().isEmpty());
    }

    /** Called by LeadsViewController after showInShell(View.LEAD_DETAIL). Loads via SSD editLead(leadId) + selectLead(leadId). */
    public void setLead(Long leadId) {
        this.leadId = leadId;
        // TODO(Ismail): lead = leadController.editLead(leadId); companyLabel, timelineList <- lead.getHistory(),
        //  draftsList <- lead.getDrafts(); statusCombo value <- leadController.selectLead(leadId)
    }

    /** UC07 SSD addInteractionNote(leadId) -> NoteForm; show/focus noteArea */
    @FXML
    private void onAddNote() {
        // TODO(Ismail)
    }

    /** UC07 SSD submitInteraction(leadId, note) -> prepend returned InteractionLog to timelineList; clear noteArea */
    @FXML
    private void onSubmitNote() {
        // TODO(Ismail)
    }

    /** UC08 SSD changePipelineStatus(leadId, newStatus) -> PipelineMetrics; InvalidStateTransitionException -> AlertHelper.error + revert combo to selectLead(leadId) */
    @FXML
    private void onStatusChanged() {
        // TODO(Ismail)
    }

    /** UC10: open View.DRAFT_REVIEW dialog, controller.setDraft(selected draft id) */
    @FXML
    private void onOpenDraft() {
        // TODO(Ismail)
    }
}
