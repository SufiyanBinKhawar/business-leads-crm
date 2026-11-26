package com.blms.controller;

import com.blms.domain.IdealCustomerProfile;
import com.blms.domain.Workspace;
import com.blms.dto.ImportSummary;
import com.blms.exception.DataAccessException;
import com.blms.exception.IntegrationException;
import com.blms.exception.ValidationException;
import com.blms.integration.source.B2BApiLeadSource;
import com.blms.integration.source.CsvLeadSource;
import com.blms.integration.source.LeadSource;
import com.blms.persistence.WorkspaceRepository;
import com.blms.service.BackgroundTaskRunner;
import com.blms.service.LeadImportService;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * D5 Figure 3 «Controller» LeadIngestionController - UC04 Import Leads via CSV (SD04), UC05 Discover Leads via
 * B2B API (SD05). Associations: +runner 1 BackgroundTaskRunner, +workspace 1 Workspace, +importer 1 LeadImportService;
 * «create» CsvLeadSource and B2BApiLeadSource.
 */
public class LeadIngestionController {

    private final BackgroundTaskRunner runner;
    private final Workspace workspace;
    private final LeadImportService importer;
    /** Gap G4: persists the ApiCallLog that Workspace.logApiCall creates in SD05 (not drawn in D5). */
    private final WorkspaceRepository workspaces;
    private final String b2bApiKey;
    private CsvLeadSource csvSource;
    /** Written by the worker thread (UC05), read by the JavaFX thread - hence volatile. */
    private volatile ImportSummary lastImportSummary;

    private static final String PROVIDER = "Apollo";
    private static final String OPERATION = "GET /people/search";
    private static final Pattern HTTP_STATUS = Pattern.compile("HTTP (\\d{3})");

    public LeadIngestionController(BackgroundTaskRunner runner, Workspace workspace, LeadImportService importer,
                                   WorkspaceRepository workspaces, String b2bApiKey) {
        this.runner = runner;
        this.workspace = workspace;
        this.importer = importer;
        this.workspaces = workspaces;
        this.b2bApiKey = b2bApiKey;
    }

    /** SSD/SD04 steps 1-5: selectCsvFile(csvFile) -> requestColumnMapping(headers). */
    public String[] selectCsvFile(File csvFile) {
        csvSource = new CsvLeadSource(csvFile);   // SD04 step 2 «create»
        return csvSource.readHeaders();           // SD04 steps 3-4
    }

    /**
     * SSD/SD04 steps 6-18: confirmMappingAndImport(columnMap) -> importSummary(imported, duplicates, invalid).
     * Synchronous in D5; the UI calls it inside a JavaFX Task to honour UC04's background-thread requirement (gap G7).
     */
    public ImportSummary confirmMappingAndImport(Map<String, String> columnMap) {
        if (csvSource == null) {
            throw new ValidationException("Choose a CSV file first");
        }
        csvSource.setColumnMap(columnMap);                                     // SD04 step 7
        lastImportSummary = importer.importFrom(csvSource, workspace);         // SD04 steps 8-17
        return lastImportSummary;                                               // SD04 step 18
    }

    /**
     * SSD/SD05 steps 1-7: runApiDiscovery() -> discoveryStarted(jobId).
     * icp = workspace.getIcp() (null -> ValidationException "Configure the workspace first" = UC05 precondition);
     * src = new B2BApiLeadSource(b2bApiKey, icp); return runner.submit(() -> runDiscovery(src)).
     */
    public Long runApiDiscovery() {
        IdealCustomerProfile icp = workspace.getIcp();                         // SD05 steps 2-3
        if (icp == null) {                                                     // D3 UC05 precondition
            throw new ValidationException("Configure the workspace first - no Ideal Customer Profile has been "
                    + "generated yet (Workspace Settings)");
        }
        B2BApiLeadSource src = new B2BApiLeadSource(b2bApiKey, icp);           // SD05 step 4 «create»
        return runner.submit(() -> runDiscovery(src));                         // SD05 steps 5-7
    }

    /**
     * SD05 steps 8-25 (background thread): lastImportSummary = importer.importFrom(source, workspace);
     * workspace.logApiCall("Apollo", "GET /people/search", 200, null); workspaces.save(workspace).
     * On IntegrationException: logApiCall with the error code/message, save, rethrow (UC05 extensions).
     */
    private void runDiscovery(LeadSource source) {
        String provider = b2bApiKey == null || b2bApiKey.isBlank() ? PROVIDER + " (offline demo)" : PROVIDER;
        try {
            lastImportSummary = importer.importFrom(source, workspace);       // SD05 steps 9-22
            workspace.logApiCall(provider, OPERATION, 200, null);              // SD05 steps 23-25
            workspaces.save(workspace);                                        // gap G4: persist the ApiCallLog
        } catch (IntegrationException e) {                                     // UC05 extensions (429 / timeout)
            workspace.logApiCall(provider, OPERATION, httpStatusOf(e.getMessage()), e.getMessage());
            try {
                workspaces.save(workspace);
            } catch (DataAccessException saveFailure) {
                e.addSuppressed(saveFailure);
            }
            throw e;
        }
    }

    /** "... (HTTP 429) ..." -> 429; 0 when the call never got an HTTP answer (timeout, no network). */
    private static int httpStatusOf(String message) {
        Matcher m = HTTP_STATUS.matcher(message == null ? "" : message);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /** NOT IN D5 - approved addition (gap G1): result for importSummary / discoveryCompleted(newLeadCount, ids). */
    public ImportSummary getLastImportSummary() {
        return lastImportSummary;
    }
}
