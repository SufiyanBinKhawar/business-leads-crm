package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.ExportController;
import com.blms.dto.ExportCriteria;
import com.blms.exception.BlmsException;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;

import java.io.File;

/** OWNER: Ismail. UC12 Export Outreach Results. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class ExportViewController {

    @FXML private ComboBox<ExportCriteria> criteriaCombo;

    private ExportController exportController;

    @FXML
    private void initialize() {
        exportController = AppContext.get().exportController();
        criteriaCombo.getItems().setAll(exportController.openExportOptions());   // SSD openExportOptions()
        if (!criteriaCombo.getItems().isEmpty()) {
            criteriaCombo.getSelectionModel().selectFirst();
        }
    }

    /** UC12 SSD requestExport(criteria) -> FileChooser save -> SSD chooseDestination(path). */
    @FXML
    private void onExport() {
        ExportCriteria criteria = criteriaCombo.getValue();
        if (criteria == null) {
            AlertHelper.warn("Export", "Please select export criteria first.");
            return;
        }
        try {
            exportController.requestExport(criteria);                  // UC12 extension: empty -> warning, no file
        } catch (BlmsException e) {
            AlertHelper.warn("Export", e.getMessage());
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Exported Leads");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("leads_export.csv");
        while (true) {
            File destination = chooser.showSaveDialog(SceneManager.primaryStage());
            if (destination == null) {
                return;
            }
            try {
                int exportedRows = exportController.chooseDestination(destination.getAbsolutePath());
                AlertHelper.info("Export Complete", exportedRows + " leads exported to " + destination.getName());
                return;
            } catch (BlmsException e) {                               // UC12 extension: write denied
                AlertHelper.warn("Export", e.getMessage());
                if (destination.getParentFile() != null && destination.getParentFile().isDirectory()) {
                    chooser.setInitialDirectory(destination.getParentFile());
                }
            }
        }
    }
}
