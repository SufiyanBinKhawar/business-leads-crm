package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadIngestionController;
import com.blms.dto.ImportSummary;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.util.AlertHelper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** OWNER: Ismail. UC04 Import Leads via CSV. Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only. */
public class CsvImportViewController {

    @FXML private Label fileLabel;
    @FXML private GridPane mappingGrid;   // one ComboBox<String> (CSV header) per system field
    @FXML private ProgressBar importProgress;
    @FXML private Button importButton;

    private LeadIngestionController leadIngestionController;
    private File selectedFile;

    @FXML
    private void initialize() {
        leadIngestionController = AppContext.get().leadIngestionController();
        importButton.setDisable(true);
    }

    /** UC04 SSD selectCsvFile(file) -> String[] headers: FileChooser (*.csv); fill mapping combos. */
    @FXML
    private void onChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Leads CSV File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = chooser.showOpenDialog(SceneManager.primaryStage());
        if (file == null) {
            return;
        }

        this.selectedFile = file;
        fileLabel.setText(file.getName() + " (" + file.length() + " bytes)");

        try {
            leadIngestionController.selectCsvFile(file);
            importButton.setDisable(false);
        } catch (UnsupportedOperationException e) {
            AlertHelper.info("CSV Import (UC04)", "CSV header inspection is under active development by Sufiyan.");
            importButton.setDisable(false);
        }
    }

    /** UC04 SSD confirmMappingAndImport(map) inside a JavaFX Task (gap G7, UC04 background requirement). */
    @FXML
    private void onImport() {
        if (selectedFile == null) {
            AlertHelper.warn("CSV Import", "Please choose a CSV file first.");
            return;
        }

        importProgress.setVisible(true);
        importButton.setDisable(true);

        Service<ImportSummary> service = new Service<>() {
            @Override
            protected Task<ImportSummary> createTask() {
                return new Task<>() {
                    @Override
                    protected ImportSummary call() {
                        Map<String, String> mapping = new HashMap<>();
                        return leadIngestionController.confirmMappingAndImport(mapping);
                    }
                };
            }
        };

        service.setOnSucceeded(e -> {
            importProgress.setVisible(false);
            importButton.setDisable(false);
            ImportSummary summary = service.getValue();
            if (summary != null) {
                AlertHelper.info("Import Summary", String.format("Successfully imported %d leads (%d duplicates skipped, %d invalid).",
                        summary.imported(), summary.duplicates(), summary.invalid()));
            }
        });

        service.setOnFailed(e -> {
            importProgress.setVisible(false);
            importButton.setDisable(false);
            Throwable ex = service.getException();
            if (ex instanceof UnsupportedOperationException || (ex != null && ex.getCause() instanceof UnsupportedOperationException)) {
                AlertHelper.info("CSV Import (UC04)", "CSV lead ingestion is under active development by Sufiyan.");
            } else {
                AlertHelper.error(ex);
            }
        });

        service.start();
    }
}
