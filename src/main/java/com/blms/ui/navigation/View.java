package com.blms.ui.navigation;

/** OWNER: Ismail. Every screen in the app (UI layer - not part of D5). Adding a screen = add a constant + an FXML file. */
public enum View {
    LOGIN("login.fxml", "Login"),                                   // UC01
    MAIN_SHELL("main_shell.fxml", "Business Leads CRM"),            // sidebar + content area
    DASHBOARD("dashboard.fxml", "Pipeline Dashboard"),              // UC05, UC08 metrics, UC12 entry
    LEADS("leads.fxml", "Leads"),                                   // UC06, UC09/UC11 batch actions
    LEAD_FORM("lead_form.fxml", "Lead"),                            // UC03 add, UC06 edit (dialog)
    LEAD_DETAIL("lead_detail.fxml", "Lead Profile"),                // UC07 timeline, UC08 status, UC10 drafts
    CSV_IMPORT("csv_import.fxml", "Import Leads from CSV"),         // UC04 (dialog)
    DRAFT_REVIEW("draft_review.fxml", "Review Draft"),              // UC10 (dialog)
    WORKSPACE_SETTINGS("workspace_settings.fxml", "Workspace Settings"), // UC02
    EXPORT("export.fxml", "Export Data");                           // UC12 (dialog)

    private final String fxmlFile;
    private final String title;

    View(String fxmlFile, String title) {
        this.fxmlFile = fxmlFile;
        this.title = title;
    }

    public String getFxmlPath() {
        return "/com/blms/ui/fxml/" + fxmlFile;
    }

    public String getTitle() {
        return title;
    }
}
