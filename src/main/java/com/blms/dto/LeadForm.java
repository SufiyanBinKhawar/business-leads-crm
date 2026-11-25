package com.blms.dto;

/** Return type of LeadController.requestNewLeadForm (D5 Fig 2) = SSD UC03 blankLeadForm(). */
public record LeadForm(String companyName, String contactName, String email, String jobRole) {

    public static LeadForm blank() {
        return new LeadForm("", "", "", "");
    }
}
