package com.blms.controller;

import com.blms.TestData;
import com.blms.domain.DraftStatus;
import com.blms.domain.EmailDraft;
import com.blms.domain.Lead;
import com.blms.domain.OutreachDispatch;
import com.blms.domain.PipelineStatus;
import com.blms.domain.Workspace;
import com.blms.dto.LeadData;
import com.blms.dto.SmtpCredentials;
import com.blms.exception.IntegrationException;
import com.blms.exception.ValidationException;
import com.blms.integration.mail.EmailSender;
import com.blms.persistence.DraftRepository;
import com.blms.persistence.LeadRepository;
import com.blms.security.CredentialEncryptor;
import com.blms.service.BackgroundTaskRunner;
import com.blms.service.DraftGenerationService;
import com.blms.service.OutreachDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC09 (SD09), UC10 (SD10), UC11 (SD11) with in-memory repositories, a fake LLM and a fake mail server. */
class OutreachControllerTest {

    static class InMemoryLeads extends LeadRepository {
        final Map<Long, Lead> rows = new LinkedHashMap<>();
        int savedAll;

        InMemoryLeads() {
            super(null);
        }

        @Override
        public List<Lead> findAllByIds(List<Long> ids) {
            return ids.stream().map(rows::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public int saveAll(List<Lead> leads) {
            savedAll += leads.size();
            return leads.size();
        }
    }

    static class InMemoryDrafts extends DraftRepository {
        final Map<Long, EmailDraft> rows = new LinkedHashMap<>();
        long nextId = 1;
        long nextAttemptId = 1;

        InMemoryDrafts() {
            super(null);
        }

        @Override
        public EmailDraft save(EmailDraft d) {
            if (d.getDraftId() == null) {
                d.setDraftId(nextId++);
            }
            for (OutreachDispatch a : d.getAttempts()) {
                if (a.getDispatchId() == null) {
                    a.setDispatchId(nextAttemptId++);
                }
            }
            rows.put(d.getDraftId(), d);
            return d;
        }

        @Override
        public EmailDraft findById(Long id) {
            return rows.get(id);
        }

        @Override
        public List<EmailDraft> findApprovedByLeadIds(List<Long> ids) {
            return rows.values().stream()
                    .filter(d -> d.getStatus() == DraftStatus.APPROVED && ids.contains(d.getLeadId())).toList();
        }
    }

    /** Fake SMTP server: "bounce@" is an unknown mailbox; authFails simulates a wrong password. */
    static class FakeMailServer implements EmailSender {
        final List<String> delivered = new ArrayList<>();
        SmtpCredentials configured;
        boolean authFails;

        @Override
        public void configure(SmtpCredentials smtp) {
            configured = smtp;
        }

        @Override
        public void send(String to, String subject, String body) {
            if (authFails) {
                throw new IntegrationException(
                        "SMTP authentication failed - check the email password in Workspace Settings");
            }
            if (to.startsWith("bounce@")) {
                throw new IntegrationException("Invalid recipient: " + to);
            }
            delivered.add(to);
        }
    }

    private BackgroundTaskRunner runner;
    private Workspace workspace;
    private InMemoryLeads leads;
    private InMemoryDrafts drafts;
    private FakeMailServer mail;
    private CredentialEncryptor encryptor;
    private OutreachController controller;
    private long nextLeadId = 1;

    @BeforeEach
    void setUp() {
        runner = new BackgroundTaskRunner();
        workspace = TestData.workspace();
        leads = new InMemoryLeads();
        drafts = new InMemoryDrafts();
        mail = new FakeMailServer();
        encryptor = new CredentialEncryptor("test-key");
        DraftGenerationService generator = new DraftGenerationService(prompt -> {
            if (prompt.contains("Flaky Co")) {
                throw new IntegrationException("LLM API failure (HTTP 500)");
            }
            return "Hello from Demo\n\nHi there,\nshort pitch.";
        });
        controller = new OutreachController(runner, workspace, generator, new OutreachDispatcher(mail), drafts,
                leads, encryptor);
    }

    @AfterEach
    void tearDown() {
        runner.shutdown();
    }

    private Lead lead(String company, String email, PipelineStatus... path) {
        Lead lead = workspace.createLead(new LeadData(company, "Sara Malik", email, "CTO", "Tech", null, TestData.rep()));
        lead.setLeadId(nextLeadId++);
        lead.getPrimaryContact().setContactId(lead.getLeadId() * 10);
        for (PipelineStatus s : path) {
            lead.changeStatus(s);
        }
        leads.rows.put(lead.getLeadId(), lead);
        return lead;
    }

    /** Waits for a background job; returns its error or null. */
    private Throwable await(Long jobId) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        runner.onComplete(jobId, done::countDown, t -> {
            error.set(t);
            done.countDown();
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        return error.get();
    }

    private EmailDraft approvedDraftFor(Lead lead) {
        EmailDraft d = lead.createDraft("Hello", "Body text");
        d.approve();
        return drafts.save(d);
    }

    // ---------------------------------------------------------------- UC09

    @Test
    void uc09GeneratesForQualifiedLeadsAndReportsFailuresAtTheEnd() throws Exception {
        Lead ok = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        Lead flaky = lead("Flaky Co", "sara@flaky.example", PipelineStatus.QUALIFIED);
        Lead notQualified = lead("Early", "sara@early.example");
        assertNull(await(controller.generateDrafts(List.of(ok.getLeadId(), flaky.getLeadId(), notQualified.getLeadId()))));
        assertEquals(1, controller.getLastResultCount());
        assertEquals(2, controller.getLastFailureCount());    // LLM failure skipped + batch continued; not qualified
        EmailDraft d = drafts.rows.values().iterator().next();
        assertEquals(DraftStatus.DRAFT, d.getStatus());
        assertEquals("Hello from Demo", d.getSubject());
        assertEquals(ok.getLeadId(), d.getLeadId());
        assertEquals("sara@acme.example", d.getRecipientEmail());
    }

    @Test
    void uc09NeedsASelection() {
        assertThrows(ValidationException.class, () -> controller.generateDrafts(List.of()));
    }

    // ---------------------------------------------------------------- UC10

    @Test
    void uc10ReviseAndApprove() {
        Lead l = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        EmailDraft d = drafts.save(l.createDraft("Hi", "Draft body"));
        assertEquals("Draft body", controller.openDraft(d.getDraftId()).getBody());
        DraftStatus s = controller.approveRevisedDraft(d.getDraftId(), "Hi Sara", "Line one\n\nLine two");
        assertEquals(DraftStatus.APPROVED, s);
        assertEquals("Line one\n\nLine two", drafts.rows.get(d.getDraftId()).getBody());
        assertTrue(drafts.rows.get(d.getDraftId()).getApprovedAt() != null);
    }

    @Test
    void uc10EmptyDraftCannotBeApproved() {
        Lead l = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        EmailDraft d = drafts.save(l.createDraft("Hi", "Draft body"));
        assertThrows(ValidationException.class, () -> controller.approveRevisedDraft(d.getDraftId(), "Hi", "   "));
        assertEquals(DraftStatus.DRAFT, drafts.rows.get(d.getDraftId()).getStatus());
        assertThrows(ValidationException.class, () -> controller.openDraft(999L));
    }

    // ---------------------------------------------------------------- UC11

    @Test
    void uc11SendsMarksSentAndMovesLeadsToContacted() throws Exception {
        Lead a = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        Lead b = lead("Beta", "ali@beta.example", PipelineStatus.QUALIFIED);
        approvedDraftFor(a);
        approvedDraftFor(b);
        assertNull(await(controller.sendApprovedEmails(List.of(a.getLeadId(), b.getLeadId()))));
        assertEquals(2, controller.getLastResultCount());
        assertEquals(List.of("sara@acme.example", "ali@beta.example"), mail.delivered);
        assertEquals(PipelineStatus.CONTACTED, a.getStatus());
        assertEquals(PipelineStatus.CONTACTED, b.getStatus());
        assertEquals(2, leads.savedAll);
        drafts.rows.values().forEach(d -> {
            assertEquals(DraftStatus.SENT, d.getStatus());
            assertEquals("SENT", d.getAttempts().get(0).getDeliveryStatus());
        });
    }

    @Test
    void uc11InvalidRecipientIsLoggedAndKeepsTheDraft() throws Exception {
        Lead good = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        Lead bad = lead("Bounce Ltd", "bounce@bounce.example", PipelineStatus.QUALIFIED);
        approvedDraftFor(good);
        EmailDraft badDraft = approvedDraftFor(bad);
        assertNull(await(controller.sendApprovedEmails(List.of(good.getLeadId(), bad.getLeadId()))));
        assertEquals(1, controller.getLastResultCount());
        assertEquals(1, controller.getLastFailureCount());
        assertEquals(DraftStatus.APPROVED, badDraft.getStatus());                 // retained (gap G14)
        OutreachDispatch failed = badDraft.getAttempts().get(0);
        assertEquals("FAILED", failed.getDeliveryStatus());
        assertTrue(failed.getErrorMessage().contains("Invalid recipient"));
        assertTrue(failed.getDispatchId() != null);                               // persisted
        assertEquals(PipelineStatus.QUALIFIED, bad.getStatus());
        assertEquals(PipelineStatus.CONTACTED, good.getStatus());
    }

    @Test
    void uc11SmtpAuthenticationErrorAbortsAndAlertsTheUser() throws Exception {
        Lead a = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        EmailDraft d = approvedDraftFor(a);
        mail.authFails = true;
        Throwable error = await(controller.sendApprovedEmails(List.of(a.getLeadId())));
        assertInstanceOf(IntegrationException.class, error);
        assertTrue(error.getMessage().contains("check the email password in Workspace Settings"));
        assertEquals(DraftStatus.APPROVED, d.getStatus());
        assertEquals(PipelineStatus.QUALIFIED, a.getStatus());
        assertTrue(mail.delivered.isEmpty());
    }

    @Test
    void uc11DecryptsTheStoredPasswordOnlyForTheSend() throws Exception {
        workspace.updateDetails("Demo", "Tech", "Software");
        workspace.setSmtpCredentials("smtp.demo.example", 587, "sales@demo.example", encryptor.encrypt("app-pw"));
        Lead a = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        approvedDraftFor(a);
        assertNull(await(controller.sendApprovedEmails(List.of(a.getLeadId()))));
        assertEquals("app-pw", mail.configured.password());                      // gap G2
        assertTrue(!workspace.getSmtpCredentialEncrypted().equals("app-pw"));
    }

    @Test
    void uc11WithoutApprovedDraftsReportsIt() throws Exception {
        Lead a = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        assertInstanceOf(ValidationException.class, await(controller.sendApprovedEmails(List.of(a.getLeadId()))));
        assertThrows(ValidationException.class, () -> controller.sendApprovedEmails(null));
    }

    @Test
    void aSentDraftCannotBeRevisedAgain() throws Exception {
        Lead a = lead("Acme", "sara@acme.example", PipelineStatus.QUALIFIED);
        EmailDraft d = approvedDraftFor(a);
        assertNull(await(controller.sendApprovedEmails(List.of(a.getLeadId()))));
        assertThrows(ValidationException.class, () -> controller.approveRevisedDraft(d.getDraftId(), "x", "y"));
    }
}
