package com.blms.domain;

import com.blms.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** D5 Figure 1 Contact - composed by Lead (has contacts, +contacts 0..*); receives outreach (+dispatches 0..*). */
public class Contact {

    private Long contactId;
    private String fullName;
    private String email;
    private String jobRole;
    private String phone;
    private final List<OutreachDispatch> dispatches = new ArrayList<>();

    /** SD03 step 8: «create» Contact(contactName, email, jobRole). */
    public Contact(String fullName, String email, String jobRole) {
        this(null, fullName, email, jobRole, null);
    }

    public Contact(Long contactId, String fullName, String email, String jobRole, String phone) {
        this.contactId = contactId;
        this.fullName = Validator.requireText(fullName, "Contact name");
        this.email = Validator.requireEmail(email);
        this.jobRole = jobRole;
        this.phone = phone;
    }

    /** Called by Lead.applyUpdates (UC06). */
    void update(String fullName, String email, String jobRole, String phone) {
        this.fullName = Validator.requireText(fullName, "Contact name");
        this.email = Validator.requireEmail(email);
        this.jobRole = jobRole;
        this.phone = phone;
    }

    void addDispatch(OutreachDispatch dispatch) {
        dispatches.add(dispatch);
    }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getJobRole() { return jobRole; }
    public String getPhone() { return phone; }
    public List<OutreachDispatch> getDispatches() { return Collections.unmodifiableList(dispatches); }
}
