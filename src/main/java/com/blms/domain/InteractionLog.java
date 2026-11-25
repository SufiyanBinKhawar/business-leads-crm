package com.blms.domain;

import com.blms.util.Validator;

import java.time.LocalDateTime;

/** D5 Figure 1 InteractionLog - composed by Lead (+history 0..*); written by +author 1 UserAccount. */
public class InteractionLog {

    private Long interactionId;
    private final String note;
    private final LocalDateTime createdAt;
    private final UserAccount author;

    /** SD07 step 9: «create» InteractionLog(note, now(), author = user). */
    public InteractionLog(String note, LocalDateTime createdAt, UserAccount author) {
        this(null, note, createdAt, author);
    }

    public InteractionLog(Long interactionId, String note, LocalDateTime createdAt, UserAccount author) {
        this.interactionId = interactionId;
        this.note = Validator.requireText(note, "Note");   // UC07 extension: empty note
        this.createdAt = createdAt;
        this.author = author;
    }

    public Long getInteractionId() { return interactionId; }
    public void setInteractionId(Long interactionId) { this.interactionId = interactionId; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UserAccount getAuthor() { return author; }
}
