package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadController;
import com.blms.controller.LeadIngestionController;
import javafx.fxml.FXML;

/** OWNER: Ismail. UC05 Discover Leads via B2B API, UC08 metrics, UC12 entry point. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class DashboardViewController {

    @FXML private javafx.scene.layout.FlowPane metricsPane;   // one card per PipelineMetrics count
    @FXML private javafx.scene.control.Button runDiscoveryButton;
    @FXML private javafx.scene.control.ProgressIndicator discoveryProgress;

    private LeadController leadController;
    private LeadIngestionController leadIngestionController;

    @FXML
    private void initialize() {
        leadController = AppContext.get().leadController();
        leadIngestionController = AppContext.get().leadIngestionController();
        // TODO(Ismail): refreshMetrics();
    }

    /** render leadController.getPipelineMetrics() getters as cards (UC08 step 5) */
    @FXML
    private void refreshMetrics() {
        // TODO(Ismail)
    }

    /** UC05 SSD runApiDiscovery() -> jobId; disable button, show progress; FxAsync.onComplete(jobId, ok -> info(getLastImportSummary().imported() + " new leads") + refreshMetrics, AlertHelper::error) */
    @FXML
    private void onRunApiDiscovery() {
        // TODO(Ismail)
    }

    /** UC12: SceneManager.openDialog(View.EXPORT).stage().showAndWait() */
    @FXML
    private void onExportData() {
        // TODO(Ismail)
    }
}
