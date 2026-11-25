package com.blms.dto;

import com.blms.domain.PipelineStatus;

/** D5 type: requestExport(criteria : ExportCriteria); SD12 step 4 calls criteria.toFilter(). status == null = "All Leads". */
public record ExportCriteria(String label, PipelineStatus status) {

    public static ExportCriteria allLeads() {
        return new ExportCriteria("All Leads", null);
    }

    public static ExportCriteria only(PipelineStatus status) {
        return new ExportCriteria("Only " + status.name() + " leads", status);
    }

    public LeadFilter toFilter() {
        return new LeadFilter(null, status, null, false);
    }

    @Override
    public String toString() {
        return label;
    }
}
