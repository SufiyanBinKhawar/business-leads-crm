package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadController;
import com.blms.domain.Contact;
import com.blms.domain.EmailDraft;
import com.blms.domain.InteractionLog;
import com.blms.domain.Lead;
import com.blms.domain.OutreachDispatch;
import com.blms.domain.PipelineMetrics;
import com.blms.domain.PipelineStatus;
import com.blms.exception.BlmsException;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.navigation.View;
import com.blms.ui.util.AlertHelper;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.time.format.DateTimeFormatter;

/**
 * OWNER: Ismail (completed by Sufiyan under CONFLICTS.md S-006). UC07 Log Manual Interaction, UC08 Update Pipeline
 * Status, entry to UC10 drafts. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only.
 */
public class LeadDetailViewController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML private Label companyLabel;
    @FXML private Label contactLabel;
    @FXML private Label statusInfoLabel;
    @FXML private ComboBox<PipelineStatus> statusCombo;
    @FXML private ListView<InteractionLog> timelineList;
    @FXML private ListView<EmailDraft> draftsList;
    @FXML private TextArea noteArea;
    @FXML private Button submitNoteButton;
    @FXML private Button reviewDraftButton;

    private LeadController leadController;
    private Long leadId;
    private PipelineStatus currentStatus;
    private boolean updatingCombo;

    @FXML
    private void initialize() {
        leadController = AppContext.get().leadController();
        // UC07 extension: "System disables the 'Submit' button until text is entered" (blank text counts as empty)
        submitNoteButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> noteArea.getText() == null || noteArea.getText().isBlank(), noteArea.textProperty()));
        reviewDraftButton.disableProperty().bind(draftsList.getSelectionModel().selectedItemProperty().isNull());
        statusCombo.getItems().setAll(PipelineStatus.values());
        timelineList.setPlaceholder(new Label("No interactions logged yet"));
        draftsList.setPlaceholder(new Label("No email drafts yet - use Generate Drafts on the Leads screen (UC09)"));
        timelineList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(InteractionLog log, boolean empty) {
                super.updateItem(log, empty);
                setWrapText(true);
                setText(empty || log == null ? null
                        : WHEN.format(log.getCreatedAt()) + "  ·  " + log.getAuthor().getUsername() + "\n" + log.getNote());
            }
        });
        draftsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(EmailDraft d, boolean empty) {
                super.updateItem(d, empty);
                setWrapText(true);
                setText(empty || d == null ? null : describe(d));
            }
        });
        draftsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && draftsList.getSelectionModel().getSelectedItem() != null) {
                onOpenDraft();
            }
        });
    }

    /** Called by LeadsViewController after showInShell(View.LEAD_DETAIL). SSD editLead(leadId) + selectLead(leadId). */
    public void setLead(Long leadId) {
        this.leadId = leadId;
        try {
            Lead lead = leadController.editLead(leadId);
            companyLabel.setText(lead.getCompanyName());
            Contact c = lead.getContacts().isEmpty() ? null : lead.getPrimaryContact();
            contactLabel.setText(c == null ? "" : c.getFullName()
                    + (c.getJobRole() == null ? "" : " (" + c.getJobRole() + ")") + "  ·  " + c.getEmail()
                    + (lead.getIndustry() == null ? "" : "  ·  " + lead.getIndustry())
                    + (lead.getPhone() == null ? "" : "  ·  " + lead.getPhone())
                    + (lead.isArchived() ? "  ·  ARCHIVED" : ""));
            timelineList.getItems().setAll(lead.getHistory());
            draftsList.getItems().setAll(lead.getDrafts());
            showStatus(leadController.selectLead(leadId));                         // UC08 SSD selectLead
        } catch (BlmsException e) {
            AlertHelper.error(e);
        }
    }

    /** UC07 SSD addInteractionNote(leadId) -> NoteForm: show the text area ready for typing. */
    @FXML
    private void onAddNote() {
        try {
            leadController.addInteractionNote(leadId);
            noteArea.setPromptText("Summary of the call / meeting with " + companyLabel.getText());
            noteArea.requestFocus();
        } catch (BlmsException e) {
            AlertHelper.error(e);
        }
    }

    /** UC07 SSD submitInteraction(leadId, note) -> InteractionLog: prepend it to the timeline. */
    @FXML
    private void onSubmitNote() {
        try {
            InteractionLog log = leadController.submitInteraction(leadId, noteArea.getText());
            timelineList.getItems().add(0, log);
            noteArea.clear();
        } catch (BlmsException e) {
            AlertHelper.error(e);
        }
    }

    /**
     * UC08 SSD changePipelineStatus(leadId, newStatus) -> PipelineMetrics.
     * Extension: InvalidStateTransitionException -> message, and the combo box reverts to the stored status.
     */
    @FXML
    private void onStatusChanged() {
        PipelineStatus chosen = statusCombo.getValue();
        if (updatingCombo || chosen == null || chosen == currentStatus) {
            return;
        }
        try {
            PipelineMetrics m = leadController.changePipelineStatus(leadId, chosen);
            showStatus(chosen);
            statusInfoLabel.setText("Moved to " + chosen + "  ·  pipeline: " + m.getQualifiedCount() + " qualified, "
                    + m.getContactedCount() + " contacted, " + m.getClosedWonCount() + " won");
        } catch (BlmsException e) {
            AlertHelper.error(e);
            try {
                showStatus(leadController.selectLead(leadId));                     // revert the UI
            } catch (BlmsException reloadFailure) {
                showStatus(currentStatus);
            }
        }
    }

    /** UC10: open View.DRAFT_REVIEW for the selected draft, then reload the lead (status may have changed). */
    @FXML
    private void onOpenDraft() {
        EmailDraft selected = draftsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.warn("Review Draft", "Select a draft first.");
            return;
        }
        SceneManager.DialogHandle<DraftReviewViewController> handle = SceneManager.openDialog(View.DRAFT_REVIEW);
        handle.controller().setDraft(selected.getDraftId());
        handle.stage().showAndWait();
        setLead(leadId);
    }

    @FXML
    private void onBack() {
        SceneManager.showInShell(View.LEADS);
    }

    private void showStatus(PipelineStatus status) {
        updatingCombo = true;
        try {
            currentStatus = status;
            statusCombo.setValue(status);
        } finally {
            updatingCombo = false;
        }
    }

    /** "[APPROVED] Subject" + one line per send attempt (gap G14: failed attempts shown with their error). */
    private static String describe(EmailDraft d) {
        StringBuilder sb = new StringBuilder("[" + d.getStatus() + "]  " + d.getSubject()
                + "\n   to " + d.getRecipientEmail() + "  ·  created " + WHEN.format(d.getCreatedAt()));
        for (OutreachDispatch a : d.getAttempts()) {
            sb.append("\n   ").append(a.getDeliveryStatus()).append(" ").append(WHEN.format(a.getSentAt()));
            if (a.getErrorMessage() != null) {
                sb.append(" - ").append(a.getErrorMessage());
            }
        }
        return sb.toString();
    }
}
