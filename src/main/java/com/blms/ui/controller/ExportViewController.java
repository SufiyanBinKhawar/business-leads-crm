package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.ExportController;
import com.blms.dto.ExportCriteria;
import javafx.fxml.FXML;

/** OWNER: Ismail. UC12 Export Outreach Results. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class ExportViewController {

    @FXML private javafx.scene.control.ComboBox<ExportCriteria> criteriaCombo;

    private ExportController exportController;

    @FXML
    private void initialize() {
        exportController = AppContext.get().exportController();
        criteriaCombo.getItems().setAll(exportController.openExportOptions());   // SSD openExportOptions()
    }

    /** UC12 SSD requestExport(criteria) (ValidationException 'export is empty' -> warn, stop); FileChooser save -> SSD chooseDestination(path) -> info(count + ' rows exported'); write error -> ask for another folder */
    @FXML
    private void onExport() {
        // TODO(Ismail)
    }
}
