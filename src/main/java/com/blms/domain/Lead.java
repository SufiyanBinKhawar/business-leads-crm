package com.blms.domain;

import com.blms.domain.state.DiscoveredState;
import com.blms.domain.state.PipelineState;
import com.blms.dto.LeadData;
import com.blms.dto.LeadFilter;
import com.blms.util.Validator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D5 Figure 1 «Information Expert» Lead.
 * Composes Contact (+contacts), InteractionLog (+history), EmailDraft (+drafts) and its PipelineState (+state 1),
 * so it CREATES them (GRASP Creator). Created by +creator 1 UserAccount.
 */
public class Lead {

    private Long leadId;
    private String companyName;
    private String industry;
    private String phone;
    private boolean isArchived;
    private final LocalDateTime createdAt;
    private final UserAccount creator;
    private PipelineState state;
    private final List<Contact> contacts = new ArrayList<>();
    private final List<InteractionLog> history = new ArrayList<>();
    private final List<EmailDraft> drafts = new ArrayList<>();

    /** SD03 steps 7-9: «create» Lead(data) -> «create» Contact(...) -> «create» DiscoveredState(). */
    public Lead(LeadData data) {
        this.companyName = Validator.requireText(data.companyName(), "Company name");
        this.industry = data.industry();
        this.phone = data.phone();
        this.isArchived = false;
        this.createdAt = LocalDateTime.now();
        this.creator = data.creator();
        this.contacts.add(new Contact(data.contactName(), data.email(), data.jobRole()));
        this.state = new DiscoveredState();
    }

    /** Used by LeadRepository when loading a lead from the database. */
    public Lead(Long leadId, String companyName, String industry, String phone, boolean isArchived,
                LocalDateTime createdAt, UserAccount creator, PipelineState state) {
        this.leadId = leadId;
        this.companyName = companyName;
        this.industry = industry;
        this.phone = phone;
        this.isArchived = isArchived;
        this.createdAt = createdAt;
        this.creator = creator;
        this.state = state;
    }

    /** SD06 step 13 (UC06): Lead changes its own fields. */
    public void applyUpdates(LeadData data) {
        this.companyName = Validator.requireText(data.companyName(), "Company name");
        this.industry = data.industry();
        this.phone = data.phone();
        getPrimaryContact().update(data.contactName(), data.email(), data.jobRole(), data.phone());
    }

    /** SD06 step 14: soft delete - sets isArchived = true, never a hard delete. */
    public void archive() {
        this.isArchived = true;
    }

    /** SD08 steps 12-17: delegates the rule to the current state (State pattern). */
    public void changeStatus(PipelineStatus newStatus) {
        this.state = state.next(newStatus);
    }

    /** SD08 steps 4-7. */
    public PipelineStatus getStatus() {
        return state.getStatus();
    }

    /** SD07 steps 8-10: Creator of InteractionLog. */
    public InteractionLog addInteraction(String note, UserAccount author) {
        InteractionLog log = new InteractionLog(note, LocalDateTime.now(), author);
        history.add(log);
        return log;
    }

    /** SD09 steps 12-14: Creator of EmailDraft, addressed to this lead's contact. */
    public EmailDraft createDraft(String subject, String body) {
        EmailDraft draft = new EmailDraft(subject, body, DraftStatus.DRAFT, getPrimaryContact());
        draft.setLeadId(leadId);
        drafts.add(draft);
        return draft;
    }

    /** UC06 filter test (Information Expert: the lead knows its own fields). */
    public boolean matches(LeadFilter filter) {
        if (isArchived && !filter.includeArchived()) {
            return false;
        }
        boolean industryOk = filter.industry() == null || filter.industry().equalsIgnoreCase(industry);
        boolean statusOk = filter.status() == null || filter.status() == getStatus();
        String q = filter.searchText() == null ? "" : filter.searchText().toLowerCase();
        boolean textOk = q.isBlank() || companyName.toLowerCase().contains(q)
                || contacts.stream().anyMatch(c -> c.getFullName().toLowerCase().contains(q)
                || c.getEmail().contains(q));
        return industryOk && statusOk && textOk;
    }

    /** Lead 1 - 0..* Contact; D3 always creates one contact, which outreach is addressed to. */
    public Contact getPrimaryContact() {
        return contacts.get(0);
    }

    /** Used by LeadRepository when loading. */
    public void addLoadedContact(Contact contact) { contacts.add(contact); }
    public void addLoadedInteraction(InteractionLog log) { history.add(log); }
    /** Used by LeadRepository when loading a stored draft (CONFLICTS.md S-004, load helper like gap G8). */
    public void addLoadedDraft(EmailDraft draft) { drafts.add(draft); }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    public String getCompanyName() { return companyName; }
    public String getIndustry() { return industry; }
    public String getPhone() { return phone; }
    public boolean isArchived() { return isArchived; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UserAccount getCreator() { return creator; }
    public List<Contact> getContacts() { return Collections.unmodifiableList(contacts); }
    public List<InteractionLog> getHistory() { return Collections.unmodifiableList(history); }
    public List<EmailDraft> getDrafts() { return Collections.unmodifiableList(drafts); }
}
