package com.blms.domain;

import java.time.LocalDateTime;

/** D5 Figure 1 OutreachDispatch - composed by EmailDraft (+attempts 0..*); Contact receives outreach (+dispatches). */
public class OutreachDispatch {

    private Long dispatchId;
    private final LocalDateTime sentAt;
    private final String deliveryStatus;
    private String errorMessage;
    private final Contact recipient;

    /** SD11 step 15: «create» OutreachDispatch(now(), "SENT", recipient). */
    public OutreachDispatch(LocalDateTime sentAt, String deliveryStatus, Contact recipient) {
        this(null, sentAt, deliveryStatus, null, recipient);
    }

    public OutreachDispatch(Long dispatchId, LocalDateTime sentAt, String deliveryStatus, String errorMessage,
                            Contact recipient) {
        this.dispatchId = dispatchId;
        this.sentAt = sentAt;
        this.deliveryStatus = deliveryStatus;
        this.errorMessage = errorMessage;
        this.recipient = recipient;
    }

    public Long getDispatchId() { return dispatchId; }
    public void setDispatchId(Long dispatchId) { this.dispatchId = dispatchId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public String getErrorMessage() { return errorMessage; }
    /** UC11 extension "invalid recipient": the error is kept on the attempt. */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Contact getRecipient() { return recipient; }
}
