package com.blms.controller;

import com.blms.TestData;
import com.blms.domain.ApiCallLog;
import com.blms.domain.Lead;
import com.blms.domain.Session;
import com.blms.domain.Workspace;
import com.blms.dto.ImportSummary;
import com.blms.dto.SmtpCredentials;
import com.blms.exception.ValidationException;
import com.blms.persistence.LeadRepository;
import com.blms.persistence.WorkspaceRepository;
import com.blms.service.BackgroundTaskRunner;
import com.blms.service.LeadImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC04 (SD04) and UC05 (SD05, offline mode) with fake repositories - no database, no network. */
class LeadIngestionControllerTest {

    static class FakeLeadRepository extends LeadRepository {
        int saved;

        FakeLeadRepository() {
            super(null);
        }

        @Override
        public boolean existsByEmailDomain(String email) {
            return email.endsWith("@vertexlabs.example");          // already in the CRM
        }

        @Override
        public int saveAll(List<Lead> leads) {
            for (Lead l : leads) {
                l.setLeadId((long) (++saved));
            }
            return leads.size();
        }
    }

    static class FakeWorkspaceRepository extends WorkspaceRepository {
        int saves;

        FakeWorkspaceRepository() {
            super(null);
        }

        @Override
        public Workspace save(Workspace ws) {
            saves++;
            return ws;
        }
    }

    @TempDir
    Path dir;

    private BackgroundTaskRunner runner;
    private Workspace workspace;
    private FakeLeadRepository leads;
    private FakeWorkspaceRepository workspaces;
    private LeadIngestionController controller;

    @BeforeEach
    void setUp() {
        runner = new BackgroundTaskRunner();
        workspace = TestData.workspace();
        leads = new FakeLeadRepository();
        workspaces = new FakeWorkspaceRepository();
        Session session = new Session(TestData.rep());
        controller = new LeadIngestionController(runner, workspace, new LeadImportService(leads, () -> session),
                workspaces, "");
    }

    @AfterEach
    void tearDown() {
        runner.shutdown();
    }

    // ---------------------------------------------------------------- UC04

    @Test
    void importsTheSampleCsvWithSummary() {
        File sample = new File("src/main/resources/sample/leads_sample.csv");
        assertArrayEquals(new String[] {"Company", "Contact Name", "Email Address", "Job Title", "Industry", "Phone"},
                controller.selectCsvFile(sample));
        ImportSummary s = controller.confirmMappingAndImport(Map.of("companyName", "Company",
                "contactName", "Contact Name", "email", "Email Address"));
        assertEquals(3, s.imported());          // Acme, Blue Lake, Nimbus
        assertEquals(1, s.duplicates());        // Nimbus twice
        assertEquals(1, s.invalid());           // Broken Row Inc
        assertEquals(s, controller.getLastImportSummary());
    }

    @Test
    void uc04MalformedFileChangesNothing() throws Exception {
        Path bad = dir.resolve("bad.csv");
        Files.writeString(bad, "Company,Contact,Email\n\"unterminated,x,y\n");
        assertThrows(ValidationException.class, () -> controller.selectCsvFile(bad.toFile()));   // SD04 step 3
        assertThrows(ValidationException.class, () -> controller.confirmMappingAndImport(Map.of()));
        assertEquals(0, leads.saved);
    }

    @Test
    void importWithoutChoosingAFileIsRejected() {
        assertThrows(ValidationException.class, () -> controller.confirmMappingAndImport(Map.of()));
    }

    // ---------------------------------------------------------------- UC05

    @Test
    void discoveryNeedsAConfiguredIcp() {
        assertNull(workspace.getIcp());
        assertThrows(ValidationException.class, () -> controller.runApiDiscovery());
    }

    @Test
    void discoveryRunsInBackgroundImportsAndLogsTheCall() throws Exception {
        workspace.updateDetails("Demo", "Fintech", "Consulting");
        workspace.setSmtpCredentials("smtp.demo.example", 587, "u", "cipher");
        workspace.generateIcp();

        Long jobId = controller.runApiDiscovery();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runner.onComplete(jobId, done::countDown, t -> {
            failure.set(t);
            done.countDown();
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertNull(failure.get());

        ImportSummary s = controller.getLastImportSummary();
        assertEquals(1, s.imported());        // Orbit Analytics
        assertEquals(1, s.duplicates());      // Vertex Labs already exists
        assertEquals(1, s.invalid());         // Quanta Retail has no contact name (null-safe JSON)
        ApiCallLog log = workspace.getApiLogs().get(0);
        assertEquals(200, log.getStatusCode());
        assertEquals("POST /mixed_people/api_search + /people/bulk_match", log.getOperation());
        assertEquals(1, workspaces.saves);    // gap G4: the log is persisted
    }

    @Test
    void smtpIsNotNeededForDiscovery() {
        assertEquals(new SmtpCredentials(null, 587, null, null), workspace.getSmtpCredentials());
    }
}
