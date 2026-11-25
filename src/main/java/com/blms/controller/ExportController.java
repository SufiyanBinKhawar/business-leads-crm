package com.blms.controller;

import com.blms.domain.PipelineStatus;
import com.blms.dto.ExportCriteria;
import com.blms.integration.export.Exporter;
import com.blms.persistence.LeadRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * D5 Figure 3 «Controller» ExportController - UC12 Export Outreach Results (SD12).
 * Attribute: - pendingRows : List&lt;String&gt;. Associations: +leads 1 LeadRepository, +exporter 1 Exporter.
 */
public class ExportController {

    private List<String> pendingRows = List.of();
    private final LeadRepository leads;
    private final Exporter exporter;

    public ExportController(LeadRepository leads, Exporter exporter) {
        this.leads = leads;
        this.exporter = exporter;
    }

    /** SSD/SD12 steps 1-2: openExportOptions() -> exportCriteriaOptions(). */
    public ExportCriteria[] openExportOptions() {
        List<ExportCriteria> options = new ArrayList<>();
        options.add(ExportCriteria.allLeads());
        for (PipelineStatus s : PipelineStatus.values()) {
            options.add(ExportCriteria.only(s));
        }
        return options.toArray(new ExportCriteria[0]);
    }

    /**
     * SSD/SD12 steps 3-9: requestExport(criteria) -> requestSaveDestination().
     * found = leads.findByFilter(criteria.toFilter()); empty -> ValidationException("No records match - export is
     * empty") (UC12 extension, no file created); pendingRows = exporter.format(found).
     */
    public void requestExport(ExportCriteria criteria) {
        // TODO(Sufiyan, UC12)
        throw new UnsupportedOperationException("TODO UC12 requestExport");
    }

    /**
     * SSD/SD12 steps 10-12: chooseDestination(filePath) -> exportSaved(filePath, exportedCount).
     * exporter.write(pendingRows, filePath); return number of data rows (header excluded).
     */
    public int chooseDestination(String filePath) {
        // TODO(Sufiyan, UC12)
        throw new UnsupportedOperationException("TODO UC12 chooseDestination");
    }
}
