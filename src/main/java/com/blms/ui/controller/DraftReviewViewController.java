package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.OutreachController;
import com.blms.domain.DraftStatus;
import com.blms.domain.EmailDraft;
import com.blms.exception.BlmsException;
import com.blms.exception.ValidationException;
import com.blms.ui.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * OWNER: Ismail (completed by Sufiyan under CONFLICTS.md S-006). UC10 Review and Revise Drafts.
 * Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only.
 */
public class DraftReviewViewController {

    @FXML private Label recipientLabel;
    @FXML private Label statusLabel;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyArea;   // UC10: TextArea keeps newlines/paragraphs
    @FXML private Button approveButton;

    private OutreachController outreachController;
    private Long draftId;

    @FXML
    private void initialize() {
        outreachController = AppContext.get().outreachController();
    }

    /** UC10 SSD openDraft(draftId) -> EmailDraft: fill recipient, subject and body. */
    public void setDraft(Long draftId) {
        this.draftId = draftId;
        try {
            EmailDraft d = outreachController.openDraft(draftId);
            recipientLabel.setText("To: " + d.getRecipient().getFullName() + " <" + d.getRecipientEmail() + ">");
            subjectField.setText(d.getSubject());
            bodyArea.setText(d.getBody());
            boolean sent = d.getStatus() == DraftStatus.SENT;
            statusLabel.setText("Status: " + d.getStatus() + (sent ? " - already sent, read only" : ""));
            subjectField.setEditable(!sent);
            bodyArea.setEditable(!sent);
            approveButton.setDisable(sent);
        } catch (BlmsException e) {
            AlertHelper.error(e);
            close();
        }
    }

    /**
     * UC10 SSD approveRevisedDraft(draftId, subject, body) -> DraftStatus.
     * Extension "actor clears the text completely": ValidationException -> prompt for content, dialog stays open.
     */
    @FXML
    private void onApprove() {
        try {
            DraftStatus status = outreachController.approveRevisedDraft(draftId, subjectField.getText(), bodyArea.getText());
            AlertHelper.info("Draft " + status, "The draft is approved and ready to send (UC11).");
            close();
        } catch (ValidationException e) {
            AlertHelper.warn("Draft is empty", e.getMessage() + " - please enter the email content before approving.");
        } catch (BlmsException e) {
            AlertHelper.error(e);
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) approveButton.getScene().getWindow()).close();
    }
}
