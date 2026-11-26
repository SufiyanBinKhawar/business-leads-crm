package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadIngestionController;
import com.blms.dto.ImportSummary;
import com.blms.exception.BlmsException;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.util.AlertHelper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OWNER: Ismail (completed by Sufiyan under CONFLICTS.md S-006). UC04 Import Leads via CSV.
 * Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only.
 */
public class CsvImportViewController {

    /** System field (columnMap key) -> label shown to the actor. * = required. */
    private static final Map<String, String> FIELDS = new LinkedHashMap<>();
    /** System field -> header names that mean the same thing (pre-selection only; the actor confirms). */
    private static final Map<String, List<String>> HINTS = Map.of(
            "companyName", List.of("company", "companyname", "organization", "organisation", "account"),
            "contactName", List.of("contactname", "contact", "name", "fullname"),
            "email", List.of("email", "emailaddress", "e-mail", "mail"),
            "jobRole", List.of("jobtitle", "title", "jobrole", "role", "position"),
            "industry", List.of("industry", "sector"),
            "phone", List.of("phone", "phonenumber", "telephone", "mobile"));
    private static final String NOT_MAPPED = "(not mapped)";

    static {
        FIELDS.put("companyName", "Company name *");
        FIELDS.put("contactName", "Contact name *");
        FIELDS.put("email", "Email *");
        FIELDS.put("jobRole", "Job role");
        FIELDS.put("industry", "Industry");
        FIELDS.put("phone", "Phone");
    }

    @FXML private Label fileLabel;
    @FXML private GridPane mappingGrid;   // one ComboBox<String> (CSV header) per system field
    @FXML private ProgressBar importProgress;
    @FXML private Button importButton;

    private LeadIngestionController leadIngestionController;
    private final Map<String, ComboBox<String>> combos = new LinkedHashMap<>();

    @FXML
    private void initialize() {
        leadIngestionController = AppContext.get().leadIngestionController();
        importButton.setDisable(true);
    }

    /** UC04 SSD selectCsvFile(file) -> String[] headers: FileChooser (*.csv), then one mapping combo per field. */
    @FXML
    private void onChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Leads CSV File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = chooser.showOpenDialog(SceneManager.primaryStage());
        if (file == null) {
            return;
        }
        mappingGrid.getChildren().clear();
        combos.clear();
        importButton.setDisable(true);
        try {
            String[] headers = leadIngestionController.selectCsvFile(file);
            fileLabel.setText(file.getName() + "  (" + headers.length + " columns)");
            buildMappingGrid(headers);
            importButton.setDisable(false);
        } catch (BlmsException e) {                 // UC04 extensions: unsupported / malformed file
            fileLabel.setText("No file selected");
            AlertHelper.error(e);
        }
    }

    /** UC04 SSD confirmMappingAndImport(map) inside a JavaFX Task (gap G7, UC04 background requirement). */
    @FXML
    private void onImport() {
        Map<String, String> mapping = new LinkedHashMap<>();
        combos.forEach((field, combo) -> {
            String header = combo.getValue();
            if (header != null && !NOT_MAPPED.equals(header)) {
                mapping.put(field, header);
            }
        });
        Service<ImportSummary> service = new Service<>() {
            @Override
            protected Task<ImportSummary> createTask() {
                return new Task<>() {
                    @Override
                    protected ImportSummary call() {
                        return leadIngestionController.confirmMappingAndImport(mapping);
                    }
                };
            }
        };
        importProgress.setVisible(true);
        importProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        importButton.setDisable(true);
        service.setOnSucceeded(e -> {
            importProgress.setVisible(false);
            importButton.setDisable(false);
            ImportSummary s = service.getValue();
            AlertHelper.info("Import Summary", s.imported() + " imported, " + s.duplicates() + " duplicates skipped, "
                    + s.invalid() + " invalid rows skipped.");
        });
        service.setOnFailed(e -> {
            importProgress.setVisible(false);
            importButton.setDisable(false);
            AlertHelper.error(service.getException());
        });
        service.start();
    }

    private void buildMappingGrid(String[] headers) {
        int row = 0;
        for (Map.Entry<String, String> f : FIELDS.entrySet()) {
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().add(NOT_MAPPED);
            combo.getItems().addAll(headers);
            combo.setValue(guess(f.getKey(), headers));
            combo.setPrefWidth(260);
            mappingGrid.add(new Label(f.getValue()), 0, row);
            mappingGrid.add(combo, 1, row);
            combos.put(f.getKey(), combo);
            row++;
        }
    }

    private static String guess(String field, String[] headers) {
        for (String h : headers) {
            String norm = h.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "");
            if (HINTS.get(field).contains(norm)) {
                return h;
            }
        }
        return NOT_MAPPED;
    }
}
