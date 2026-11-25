package com.blms.integration.source;

import com.blms.dto.LeadData;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * D5 Figure 3 CsvLeadSource realizes LeadSource (UC04). Created by LeadIngestionController (SD04 step 2).
 * columnMap keys are the system field names: companyName, contactName, email, jobRole, industry, phone;
 * values are the CSV header chosen by the actor.
 */
public class CsvLeadSource implements LeadSource {

    private final File file;
    private Map<String, String> columnMap = new HashMap<>();

    public CsvLeadSource(File file) {
        this.file = file;
    }

    /**
     * SD04 steps 3-4. UC04 extensions: not a .csv / not UTF-8 -> ValidationException("Please choose a UTF-8 CSV file");
     * empty or unreadable file -> ValidationException before any database change.
     */
    public String[] readHeaders() {
        // TODO(Sufiyan, UC04): read first line as UTF-8, split on commas outside double quotes
        throw new UnsupportedOperationException("TODO UC04 CsvLeadSource.readHeaders");
    }

    /** SD04 step 7. */
    public void setColumnMap(Map<String, String> map) {
        this.columnMap = new HashMap<>(map);
    }

    /** SD04 steps 9-10: one LeadData per data row; malformed rows are returned too (isValid() == false). */
    @Override
    public List<LeadData> fetchLeads() {
        // TODO(Sufiyan, UC04): read every row after the header, map columns using columnMap, build LeadData
        throw new UnsupportedOperationException("TODO UC04 CsvLeadSource.fetchLeads");
    }

    public File getFile() { return file; }
}
