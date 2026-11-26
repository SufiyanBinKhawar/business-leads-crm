package com.blms.domain;

import com.blms.exception.ValidationException;
import com.blms.util.Validator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D5 Figure 1 «Information Expert» EmailDraft - composed by Lead (+drafts 0..*), addressed to +recipient 1 Contact,
 * composes its send attempts (+attempts 0..*). Revises and approves itself (SD10), creates OutreachDispatch (SD11).
 */
public class EmailDraft {

    private Long draftId;
    private String subject;
    private String body;
    private DraftStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private final Contact recipient;
    private final List<OutreachDispatch> attempts = new ArrayList<>();
    /** Persistence-only foreign key (EMAIL_DRAFTS.LEAD_ID); Lead navigates to drafts, not the reverse. */
    private Long leadId;

    /** SD09 step 13: «create» EmailDraft(subject, body, DRAFT). */
    public EmailDraft(String subject, String body, DraftStatus status, Contact recipient) {
        this(null, subject, body, status, LocalDateTime.now(), null, recipient);
    }

    public EmailDraft(Long draftId, String subject, String body, DraftStatus status, LocalDateTime createdAt,
                      LocalDateTime approvedAt, Contact recipient) {
        this.draftId = draftId;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
        this.recipient = recipient;
    }

    /** SD10 step 8. UC10 extension: an empty draft is rejected. */
    public void revise(String subject, String body) {
        if (status == DraftStatus.SENT) {
            throw new ValidationException("This email has already been sent and can no longer be edited");
        }
        this.subject = Validator.requireText(subject, "Subject");
        this.body = Validator.requireText(body, "Email body");
    }

    /** SD10 steps 9-10: status = APPROVED, approvedAt = now(). */
    public void approve() {
        Validator.requireText(body, "Email body");
        this.status = DraftStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    /** SD11 steps 11-12: resolves the address through +recipient. */
    public String getRecipientEmail() {
        return recipient.getEmail();
    }

    /** SD11 steps 14-16: Creator of OutreachDispatch; on "SENT" the draft becomes SENT. */
    public OutreachDispatch recordDispatch(String status) {
        OutreachDispatch dispatch = new OutreachDispatch(LocalDateTime.now(), status, recipient);
        attempts.add(dispatch);
        recipient.addDispatch(dispatch);
        if ("SENT".equals(status)) {
            this.status = DraftStatus.SENT;
        }
        return dispatch;
    }

    /** Used by LeadRepository when loading a stored send attempt (CONFLICTS.md S-004, load helper like gap G8). */
    public void addLoadedAttempt(OutreachDispatch attempt) {
        attempts.add(attempt);
        recipient.addDispatch(attempt);
    }

    public Long getDraftId() { return draftId; }
    public void setDraftId(Long draftId) { this.draftId = draftId; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public DraftStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public Contact getRecipient() { return recipient; }
    public List<OutreachDispatch> getAttempts() { return Collections.unmodifiableList(attempts); }
    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
}
