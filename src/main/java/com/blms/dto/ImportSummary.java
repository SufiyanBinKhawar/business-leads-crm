package com.blms.dto;

import java.util.List;

/** D5 type returned by LeadImportService.importFrom and confirmMappingAndImport = SSD UC04 importSummary(...); UC05 discoveryCompleted(newLeadCount, discoveredLeadIds). */
public record ImportSummary(int imported, int duplicates, int invalid, List<Long> importedLeadIds) {
}
