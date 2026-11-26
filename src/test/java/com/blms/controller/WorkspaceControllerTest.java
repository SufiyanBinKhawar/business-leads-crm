package com.blms.controller;

import com.blms.TestData;
import com.blms.domain.Workspace;
import com.blms.dto.SmtpCredentials;
import com.blms.dto.WorkspaceForm;
import com.blms.dto.WorkspaceResult;
import com.blms.exception.ValidationException;
import com.blms.persistence.WorkspaceRepository;
import com.blms.security.CredentialEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC02 / SD02 with a fake WorkspaceRepository - no database. */
class WorkspaceControllerTest {

    static class FakeWorkspaceRepository extends WorkspaceRepository {
        final Workspace stored;
        int saves;

        FakeWorkspaceRepository(Workspace stored) {
            super(null);
            this.stored = stored;
        }

        @Override
        public Workspace findCurrent() {
            return stored;
        }

        @Override
        public Workspace save(Workspace ws) {
            saves++;
            ws.getIcp().setIcpId(7L);
            return ws;
        }
    }

    private Workspace workspace;
    private FakeWorkspaceRepository repo;
    private CredentialEncryptor encryptor;
    private WorkspaceController controller;

    @BeforeEach
    void setUp() {
        workspace = TestData.workspace();
        repo = new FakeWorkspaceRepository(workspace);
        encryptor = new CredentialEncryptor("test-key");
        controller = new WorkspaceController(workspace, repo, encryptor);
    }

    @Test
    void openWorkspaceSettingsNeverReturnsThePassword() {
        WorkspaceForm form = controller.openWorkspaceSettings();
        assertEquals("Demo Company", form.companyName());
        assertEquals(587, form.smtpPort());
    }

    @Test
    void saveWorkspaceEncryptsGeneratesIcpAndSaves() {
        WorkspaceResult r = controller.saveWorkspace("Acme Sales", "Fintech", "Payments consulting",
                new SmtpCredentials("smtp.acme.example", 587, "sales@acme.example", "s3cret"));
        assertTrue(r.success());
        assertEquals(1L, r.workspaceId());
        assertEquals(7L, r.icpId());
        assertEquals(1, repo.saves);
        assertEquals("Fintech", workspace.getIcp().getTargetIndustry());
        String cipher = workspace.getSmtpCredentialEncrypted();
        assertNotEquals("s3cret", cipher);                          // UC02 special requirement: never plain text
        assertEquals("s3cret", encryptor.decrypt(cipher));
    }

    @Test
    void uc02MissingFieldsAreAllNamedAndNothingChanges() {
        ValidationException e = assertThrows(ValidationException.class, () -> controller.saveWorkspace(
                " ", "Fintech", "", new SmtpCredentials("smtp.acme.example", 587, "", "pw")));
        assertTrue(e.getMessage().contains("Company name"));
        assertTrue(e.getMessage().contains("Services offered"));
        assertTrue(e.getMessage().contains("SMTP username"));
        assertEquals("Demo Company", workspace.getCompanyName());
        assertNull(workspace.getIcp());
        assertEquals(0, repo.saves);
    }

    @Test
    void uc02InvalidSmtpSettingsAreRejected() {
        assertThrows(ValidationException.class, () -> controller.saveWorkspace("Acme", "Fintech", "x",
                new SmtpCredentials("smtp.acme.example", 70000, "u", "pw")));
        assertThrows(ValidationException.class, () -> controller.saveWorkspace("Acme", "Fintech", "x",
                new SmtpCredentials("https://smtp acme", 587, "u", "pw")));
        assertThrows(ValidationException.class, () -> controller.saveWorkspace("Acme", "Fintech", "x", null));
        assertEquals(0, repo.saves);
    }

    @Test
    void icpBuildsItsOwnSearchQuery() {
        controller.saveWorkspace("Acme", "Health Tech", "AI & data",
                new SmtpCredentials("smtp.acme.example", 465, "u@acme.example", "pw"));
        String q = workspace.getIcp().toSearchQuery();
        assertNotNull(q);
        assertEquals("industry=Health+Tech&keywords=AI+%26+data", q);
    }
}
