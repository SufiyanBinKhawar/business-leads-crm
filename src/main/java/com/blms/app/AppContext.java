package com.blms.app;

import com.blms.controller.ExportController;
import com.blms.controller.LeadController;
import com.blms.controller.LeadIngestionController;
import com.blms.controller.LoginController;
import com.blms.controller.OutreachController;
import com.blms.controller.WorkspaceController;
import com.blms.domain.PipelineMetrics;
import com.blms.domain.Workspace;
import com.blms.exception.DataAccessException;
import com.blms.integration.export.CsvExporter;
import com.blms.integration.llm.OpenAiLlmClient;
import com.blms.integration.mail.JakartaMailSender;
import com.blms.persistence.DraftRepository;
import com.blms.persistence.LeadRepository;
import com.blms.persistence.UserRepository;
import com.blms.persistence.WorkspaceRepository;
import com.blms.security.BCryptPasswordHasher;
import com.blms.security.CredentialEncryptor;
import com.blms.service.BackgroundTaskRunner;
import com.blms.service.DraftGenerationService;
import com.blms.service.LeadImportService;
import com.blms.service.OutreachDispatcher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * OWNER: Sufiyan. Composition root - infrastructure, NOT a D5 design class.
 * The ONLY place that uses "new" on controllers, repositories, services and adapters, wiring exactly the
 * D5 associations (+hasher, +users, +leads, +runner, +importer, +generator, +dispatcher, +exporter ...).
 * UI view controllers get the six GRASP controllers from here: {@code AppContext.get().leadController()}.
 */
public final class AppContext {

    private static AppContext instance;

    private final Connection connection;
    private final BackgroundTaskRunner runner = new BackgroundTaskRunner();
    private final LoginController loginController;
    private final WorkspaceController workspaceController;
    private final LeadController leadController;
    private final LeadIngestionController leadIngestionController;
    private final OutreachController outreachController;
    private final ExportController exportController;

    private AppContext(AppConfig cfg) {
        try {
            connection = DriverManager.getConnection(cfg.get("db.url"), cfg.get("db.user"), cfg.get("db.password"));
        } catch (SQLException e) {
            throw new DataAccessException("Unable to connect to database", e);   // UC01 extension
        }
        UserRepository users = new UserRepository(connection);
        WorkspaceRepository workspaces = new WorkspaceRepository(connection);
        LeadRepository leads = new LeadRepository(connection);
        DraftRepository drafts = new DraftRepository(connection);

        Workspace workspace = workspaces.findCurrent();
        if (workspace == null) {
            throw new IllegalStateException("No workspace row - run src/main/resources/db/03_seed.sql");
        }
        CredentialEncryptor encryptor = new CredentialEncryptor(cfg.get("security.credentialKey"));

        loginController = new LoginController(new BCryptPasswordHasher(), users);
        workspaceController = new WorkspaceController(workspace, workspaces, encryptor);
        leadController = new LeadController(loginController::getSession, leads, new PipelineMetrics(), workspace);
        leadIngestionController = new LeadIngestionController(runner, workspace,
                new LeadImportService(leads, loginController::getSession), workspaces, cfg.get("b2b.apiKey"));
        outreachController = new OutreachController(runner, workspace,
                new DraftGenerationService(new OpenAiLlmClient(cfg.get("llm.apiKey"))),
                new OutreachDispatcher(new JakartaMailSender()), drafts, leads, encryptor);
        exportController = new ExportController(leads, new CsvExporter());
    }

    /** Called once by MainApp.init(). */
    public static synchronized void bootstrap() {
        if (instance == null) {
            instance = new AppContext(AppConfig.load());
        }
    }

    public static AppContext get() {
        if (instance == null) {
            throw new IllegalStateException("AppContext.bootstrap() was not called");
        }
        return instance;
    }

    public void shutdown() {
        runner.shutdown();
        try {
            connection.close();
        } catch (SQLException ignored) {
            // closing on exit - nothing useful to do
        }
    }

    public BackgroundTaskRunner taskRunner() { return runner; }
    public LoginController loginController() { return loginController; }
    public WorkspaceController workspaceController() { return workspaceController; }
    public LeadController leadController() { return leadController; }
    public LeadIngestionController leadIngestionController() { return leadIngestionController; }
    public OutreachController outreachController() { return outreachController; }
    public ExportController exportController() { return exportController; }
}
