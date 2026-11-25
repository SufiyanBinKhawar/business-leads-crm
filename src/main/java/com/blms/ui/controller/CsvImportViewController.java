package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadIngestionController;
import javafx.fxml.FXML;

/** OWNER: Ismail. UC04 Import Leads via CSV. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class CsvImportViewController {

    @FXML private javafx.scene.control.Label fileLabel;
    @FXML private javafx.scene.layout.GridPane mappingGrid;   // one ComboBox<String> (CSV header) per system field
    @FXML private javafx.scene.control.ProgressBar importProgress;
    @FXML private javafx.scene.control.Button importButton;

    private LeadIngestionController leadIngestionController;

    @FXML
    private void initialize() {
        leadIngestionController = AppContext.get().leadIngestionController();
        importButton.setDisable(true);
    }

    /** UC04 SSD selectCsvFile(file) -> String[] headers: FileChooser (*.csv); fill mapping combos for companyName, contactName, email, jobRole, industry, phone */
    @FXML
    private void onChooseFile() {
        // TODO(Ismail)
    }

    /** UC04 SSD confirmMappingAndImport(map) inside a javafx.concurrent.Task (gap G7, UC04 background requirement) -> info 'X imported, Y duplicates skipped, Z invalid' */
    @FXML
    private void onImport() {
        // TODO(Ismail)
    }
}
