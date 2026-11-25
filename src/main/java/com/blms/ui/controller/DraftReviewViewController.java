package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.OutreachController;
import javafx.fxml.FXML;

/** OWNER: Ismail. UC10 Review and Revise Drafts. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class DraftReviewViewController {

    @FXML private javafx.scene.control.Label recipientLabel;
    @FXML private javafx.scene.control.TextField subjectField;
    @FXML private javafx.scene.control.TextArea bodyArea;   // UC10: TextArea keeps newlines/paragraphs
    @FXML private javafx.scene.control.Button approveButton;

    private OutreachController outreachController;
    private Long draftId;

    @FXML
    private void initialize() {
        outreachController = AppContext.get().outreachController();
    }

    /** UC10 SSD openDraft(draftId) -> EmailDraft: fill recipientLabel (getRecipientEmail), subjectField, bodyArea. */
    public void setDraft(Long draftId) {
        this.draftId = draftId;
        // TODO(Ismail)
    }

    /** UC10 SSD approveRevisedDraft(draftId, subject, body) -> DraftStatus; ValidationException (empty) -> AlertHelper.warn; success -> close dialog */
    @FXML
    private void onApprove() {
        // TODO(Ismail)
    }
}
