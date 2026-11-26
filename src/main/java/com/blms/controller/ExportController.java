package com.blms.controller;

import com.blms.domain.Lead;
import com.blms.domain.PipelineStatus;
import com.blms.dto.ExportCriteria;
import com.blms.exception.ValidationException;
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
        if (criteria == null) {
            throw new ValidationException("Choose what to export");
        }
        List<Lead> found = leads.findByFilter(criteria.toFilter());           // SD12 steps 4-5
        if (found.isEmpty()) {                                                 // UC12 extension: no file created
            pendingRows = List.of();
            throw new ValidationException("No records match \"" + criteria.label() + "\" - the export is empty");
        }
        pendingRows = exporter.format(found);                                  // SD12 steps 6-8
    }

    /**
     * SSD/SD12 steps 10-12: chooseDestination(filePath) -> exportSaved(filePath, exportedCount).
     * exporter.write(pendingRows, filePath); return number of data rows (header excluded).
     */
    public int chooseDestination(String filePath) {
        if (pendingRows.isEmpty()) {
            throw new ValidationException("Choose the export criteria first");
        }
        exporter.write(pendingRows, filePath);     // SD12 step 11 (write denied -> UC12 extension, rows kept for retry)
        return pendingRows.size() - 1;             // SD12 step 12 exportedCount (header excluded)
    }
}
