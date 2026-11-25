package com.blms.dto;

import com.blms.domain.UserAccount;

/**
 * D5 type: Workspace.createLead(data), Lead.applyUpdates(data), LeadSource.fetchLeads() : List<LeadData>.
 * creator = the session user who caused the lead to be created (D5 Lead +creator 1; CONFLICTS.md gap G5).
 */
public record LeadData(String companyName, String contactName, String email, String jobRole,
                       String industry, String phone, UserAccount creator) {

    public LeadData withCreator(UserAccount user) {
        return new LeadData(companyName, contactName, email, jobRole, industry, phone, user);
    }

    /** "Irregular row" rule for UC04 step 4 and null-safe rule for UC05. */
    public boolean isValid() {
        return companyName != null && !companyName.isBlank()
                && contactName != null && !contactName.isBlank()
                && com.blms.util.Validator.isEmail(email);
    }
}
