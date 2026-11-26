package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadController;
import com.blms.controller.LeadIngestionController;
import com.blms.domain.PipelineMetrics;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.navigation.View;
import com.blms.dto.ImportSummary;
import com.blms.exception.BlmsException;
import com.blms.ui.util.AlertHelper;
import com.blms.ui.util.FxAsync;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** OWNER: Ismail. UC05 Discover Leads via B2B API, UC08 metrics, UC12 entry point. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class DashboardViewController {

    @FXML private FlowPane metricsPane;
    @FXML private Button runDiscoveryButton;
    @FXML private ProgressIndicator discoveryProgress;

    private LeadController leadController;
    private LeadIngestionController leadIngestionController;

    @FXML
    private void initialize() {
        leadController = AppContext.get().leadController();
        leadIngestionController = AppContext.get().leadIngestionController();
        refreshMetrics();
    }

    /** Render leadController.getPipelineMetrics() getters as cards (UC08 step 5, gap G10). */
    @FXML
    private void refreshMetrics() {
        metricsPane.getChildren().clear();
        try {
            PipelineMetrics metrics = leadController.getPipelineMetrics();
            metricsPane.getChildren().addAll(
                    createCard("Total Leads", String.valueOf(metrics.getTotalLeads())),
                    createCard("Discovered", String.valueOf(metrics.getDiscoveredCount())),
                    createCard("Qualified", String.valueOf(metrics.getQualifiedCount())),
                    createCard("Contacted", String.valueOf(metrics.getContactedCount())),
                    createCard("Closed Won", String.valueOf(metrics.getClosedWonCount())),
                    createCard("Closed Lost", String.valueOf(metrics.getClosedLostCount()))
            );
        } catch (BlmsException e) {
            AlertHelper.error(e);
        }
    }

    private VBox createCard(String title, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("metric-card");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("metric-label");
        Label lblVal = new Label(value);
        lblVal.getStyleClass().add("metric-value");
        card.getChildren().addAll(lblTitle, lblVal);
        return card;
    }

    /** UC05 SSD runApiDiscovery() -> jobId; disable button, show progress; FxAsync.onComplete. */
    @FXML
    private void onRunApiDiscovery() {
        try {
            Long jobId = leadIngestionController.runApiDiscovery();
            discoveryProgress.setVisible(true);
            runDiscoveryButton.setDisable(true);
            FxAsync.onComplete(jobId, () -> {
                discoveryProgress.setVisible(false);
                runDiscoveryButton.setDisable(false);
                ImportSummary s = leadIngestionController.getLastImportSummary();
                AlertHelper.info("API Discovery completed", s.imported() + " new lead(s) saved, " + s.duplicates()
                        + " already in the CRM, " + s.invalid() + " skipped (missing contact data).");
                refreshMetrics();
            }, error -> {
                discoveryProgress.setVisible(false);
                runDiscoveryButton.setDisable(false);
                AlertHelper.error(error);              // UC05: HTTP 429 / network timeout messages
            });
        } catch (BlmsException e) {
            AlertHelper.error(e);                      // e.g. workspace not configured (no ICP)
        }
    }

    /** UC12: show in shell or open dialog. */
    @FXML
    private void onExportData() {
        SceneManager.showInShell(View.EXPORT);
    }
}
