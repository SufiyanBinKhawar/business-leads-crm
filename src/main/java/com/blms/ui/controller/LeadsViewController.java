package com.blms.ui.controller;

import com.blms.app.AppContext;
import com.blms.controller.LeadController;
import com.blms.controller.OutreachController;
import com.blms.domain.Contact;
import com.blms.domain.Lead;
import com.blms.domain.PipelineStatus;
import com.blms.dto.LeadFilter;
import com.blms.ui.navigation.SceneManager;
import com.blms.ui.navigation.View;
import com.blms.ui.util.AlertHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.List;

/**
 * OWNER: Ismail. UC06 Manage Existing Leads screen.
 * Talks ONLY to D5 controllers from AppContext; reads domain objects via getters only.
 */
public class LeadsViewController {

    @FXML private TextField searchField;
    @FXML private TextField industryFilter;
    @FXML private ComboBox<PipelineStatus> statusFilter;
    @FXML private CheckBox showArchivedCheck;
    @FXML private Button applyFilterButton;
    @FXML private Button clearFilterButton;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private Button addLeadButton;
    @FXML private Button editLeadButton;
    @FXML private Button generateDraftsButton;
    @FXML private Button sendEmailsButton;
    @FXML private Label selectionCountLabel;

    @FXML private TableView<Lead> leadsTable;

    private LeadController leadController;
    private OutreachController outreachController;

    @FXML
    private void initialize() {
        leadController = AppContext.get().leadController();
        outreachController = AppContext.get().outreachController();

        setupFilterControls();
        setupTable();
        setupToolbarBindings();

        onApplyFilter();
    }

    private void setupFilterControls() {
        statusFilter.getItems().add(null); // null represents "All Stages"
        statusFilter.getItems().addAll(PipelineStatus.values());
        statusFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(PipelineStatus status) {
                return status == null ? "All Stages" : status.name();
            }

            @Override
            public PipelineStatus fromString(String string) {
                return null;
            }
        });
        statusFilter.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        leadsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Label emptyLabel = new Label("No leads found");
        emptyLabel.getStyleClass().add("hint");
        leadsTable.setPlaceholder(emptyLabel);

        // Columns: company name, primary contact, email, phone, industry, status, created date
        TableColumn<Lead, String> companyCol = new TableColumn<>("Company");
        companyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCompanyName()));
        companyCol.setPrefWidth(170);

        TableColumn<Lead, String> contactCol = new TableColumn<>("Primary Contact");
        contactCol.setCellValueFactory(d -> {
            Lead lead = d.getValue();
            if (lead == null || lead.getContacts().isEmpty()) {
                return new SimpleStringProperty("");
            }
            Contact c = lead.getPrimaryContact();
            return new SimpleStringProperty(c != null && c.getFullName() != null ? c.getFullName() : "");
        });
        contactCol.setPrefWidth(150);

        TableColumn<Lead, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(d -> {
            Lead lead = d.getValue();
            if (lead == null || lead.getContacts().isEmpty()) {
                return new SimpleStringProperty("");
            }
            Contact c = lead.getPrimaryContact();
            return new SimpleStringProperty(c != null && c.getEmail() != null ? c.getEmail() : "");
        });
        emailCol.setPrefWidth(180);

        TableColumn<Lead, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(d -> {
            String p = d.getValue().getPhone();
            return new SimpleStringProperty(p != null ? p : "");
        });
        phoneCol.setPrefWidth(110);

        TableColumn<Lead, String> industryCol = new TableColumn<>("Industry");
        industryCol.setCellValueFactory(d -> {
            String ind = d.getValue().getIndustry();
            return new SimpleStringProperty(ind != null ? ind : "");
        });
        industryCol.setPrefWidth(120);

        TableColumn<Lead, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> {
            PipelineStatus s = d.getValue().getStatus();
            return new SimpleStringProperty(s != null ? s.name() : "");
        });
        statusCol.setPrefWidth(110);

        TableColumn<Lead, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(d -> {
            var dt = d.getValue().getCreatedAt();
            return new SimpleStringProperty(dt != null ? dt.toLocalDate().toString() : "");
        });
        createdCol.setPrefWidth(100);

        TableColumn<Lead, String> archivedCol = new TableColumn<>("Archived");
        archivedCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isArchived() ? "Yes" : "No"));
        archivedCol.setPrefWidth(75);

        leadsTable.getColumns().setAll(companyCol, contactCol, emailCol, phoneCol, industryCol, statusCol, createdCol, archivedCol);

        // Row factory: double click to open detail + .row-archived style for archived rows
        leadsTable.setRowFactory(tv -> {
            TableRow<Lead> row = new TableRow<>() {
                @Override
                protected void updateItem(Lead lead, boolean empty) {
                    super.updateItem(lead, empty);
                    if (empty || lead == null) {
                        getStyleClass().remove("row-archived");
                    } else if (lead.isArchived()) {
                        if (!getStyleClass().contains("row-archived")) {
                            getStyleClass().add("row-archived");
                        }
                    } else {
                        getStyleClass().remove("row-archived");
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openLeadDetail(row.getItem());
                }
            });

            return row;
        });
    }

    private void setupToolbarBindings() {
        ObservableList<Lead> selected = leadsTable.getSelectionModel().getSelectedItems();

        // Edit enabled only when exactly 1 row is selected
        editLeadButton.disableProperty().bind(Bindings.size(selected).isNotEqualTo(1));

        // Generate / Send enabled only when 1+ rows are selected
        generateDraftsButton.disableProperty().bind(Bindings.isEmpty(selected));
        sendEmailsButton.disableProperty().bind(Bindings.isEmpty(selected));

        selected.addListener((ListChangeListener<Lead>) c -> {
            int size = selected.size();
            if (size == 0) {
                selectionCountLabel.setText("");
            } else if (size == 1) {
                selectionCountLabel.setText("1 lead selected");
            } else {
                selectionCountLabel.setText(size + " leads selected");
            }
        });
    }

    /**
     * UC06 SSD filterLeads(new LeadFilter(industry, status, search, showArchived)).
     * Loads asynchronously off the JavaFX thread via Service.
     */
    @FXML
    private void onApplyFilter() {
        String industry = industryFilter.getText() != null && !industryFilter.getText().isBlank()
                ? industryFilter.getText().trim() : null;
        PipelineStatus status = statusFilter.getValue();
        String search = searchField.getText() != null && !searchField.getText().isBlank()
                ? searchField.getText().trim() : null;
        boolean includeArchived = showArchivedCheck.isSelected();

        LeadFilter filter = new LeadFilter(industry, status, search, includeArchived);

        Service<List<Lead>> service = new Service<>() {
            @Override
            protected Task<List<Lead>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<Lead> call() {
                        return leadController.filterLeads(filter);
                    }
                };
            }
        };

        loadingIndicator.visibleProperty().bind(service.runningProperty());
        applyFilterButton.disableProperty().bind(service.runningProperty());

        service.setOnSucceeded(e -> {
            List<Lead> results = service.getValue();
            leadsTable.setItems(FXCollections.observableArrayList(results != null ? results : List.of()));
        });

        service.setOnFailed(e -> {
            Throwable ex = service.getException();
            AlertHelper.error(ex);
            leadsTable.setItems(FXCollections.observableArrayList());
        });

        service.start();
    }

    @FXML
    private void onClearFilter() {
        searchField.clear();
        industryFilter.clear();
        statusFilter.getSelectionModel().selectFirst();
        showArchivedCheck.setSelected(false);
        onApplyFilter();
    }

    /** UC03 SSD requestNewLeadForm(): open View.LEAD_FORM dialog in add mode. */
    @FXML
    private void onAddLead() {
        try {
            SceneManager.DialogHandle<LeadFormViewController> handle = SceneManager.openDialog(View.LEAD_FORM);
            handle.controller().openForAdd(this::onApplyFilter);
            handle.stage().showAndWait();
        } catch (Exception e) {
            AlertHelper.error(e);
        }
    }

    /** UC06 SSD editLead(leadId): open View.LEAD_FORM dialog in edit mode. */
    @FXML
    private void onEditLead() {
        Lead selected = leadsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.warn("Edit Lead", "Please select exactly one lead to edit.");
            return;
        }
        try {
            SceneManager.DialogHandle<LeadFormViewController> handle = SceneManager.openDialog(View.LEAD_FORM);
            handle.controller().openForEdit(selected.getLeadId(), this::onApplyFilter);
            handle.stage().showAndWait();
        } catch (Exception e) {
            AlertHelper.error(e);
        }
    }

    private void openLeadDetail(Lead lead) {
        if (lead == null) {
            return;
        }
        try {
            LeadDetailViewController controller = SceneManager.showInShell(View.LEAD_DETAIL);
            controller.setLead(lead.getLeadId());
        } catch (Exception e) {
            AlertHelper.error(e);
        }
    }

    /** UC09 placeholder for I-9. */
    @FXML
    private void onGenerateDrafts() {
        List<Lead> selected = leadsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            AlertHelper.warn("Generate Drafts", "Please select at least one lead.");
            return;
        }
        AlertHelper.info("Generate Drafts (UC09)", "Draft generation will be fully wired in Task I-9. Selected leads: " + selected.size());
    }

    /** UC11 placeholder for I-9. */
    @FXML
    private void onSendEmails() {
        List<Lead> selected = leadsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            AlertHelper.warn("Send Emails", "Please select at least one lead.");
            return;
        }
        AlertHelper.info("Send Emails (UC11)", "Automated outreach dispatch will be fully wired in Task I-9. Selected leads: " + selected.size());
    }
}
