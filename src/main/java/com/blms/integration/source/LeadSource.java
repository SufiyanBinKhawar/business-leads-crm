package com.blms.integration.source;

import com.blms.dto.LeadData;

import java.util.List;

/**
 * D5 Figure 3 «interface» LeadSource (Polymorphism, Protected Variations).
 * LeadImportService.importFrom() only sees this interface; fetchLeads() is bound at run time to
 * CsvLeadSource (UC04) or B2BApiLeadSource (UC05).
 */
public interface LeadSource {
    List<LeadData> fetchLeads();
}
