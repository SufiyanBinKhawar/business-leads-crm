package com.blms;

import com.blms.domain.Lead;
import com.blms.domain.UserAccount;
import com.blms.domain.UserRole;
import com.blms.domain.Workspace;
import com.blms.dto.LeadData;

/** Shared fixtures for unit tests (no database). */
public final class TestData {

    private TestData() {
    }

    public static UserAccount rep() {
        return new UserAccount(2L, "rep", "x", UserRole.SALES_REP, true);
    }

    public static Workspace workspace() {
        return new Workspace(1L, "Demo Company", "Tech", "Software", null, 587, null, null);
    }

    public static LeadData data(String company, String email) {
        return new LeadData(company, "Sara Malik", email, "CTO", "Tech", null, rep());
    }

    public static Lead lead() {
        return workspace().createLead(data("Acme", "sara@acme.example"));
    }
}
