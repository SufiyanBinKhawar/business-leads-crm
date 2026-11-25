package com.blms.dto;

import com.blms.domain.PipelineStatus;

/** D5 type: LeadController.filterLeads(criteria : LeadFilter), Lead.matches(filter). null field = "any". */
public record LeadFilter(String industry, PipelineStatus status, String searchText, boolean includeArchived) {

    public static LeadFilter all() {
        return new LeadFilter(null, null, null, false);
    }
}
