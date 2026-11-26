package com.blms.controller;

import com.blms.domain.DraftStatus;
import com.blms.domain.EmailDraft;
import com.blms.domain.Lead;
import com.blms.domain.PipelineStatus;
import com.blms.domain.Workspace;
import com.blms.dto.LeadFilter;
import com.blms.dto.SmtpCredentials;
import com.blms.exception.InvalidStateTransitionException;
import com.blms.exception.ValidationException;
import com.blms.persistence.DraftRepository;
import com.blms.persistence.LeadRepository;
import com.blms.security.CredentialEncryptor;
import com.blms.service.BackgroundTaskRunner;
import com.blms.service.DraftGenerationService;
import com.blms.service.OutreachDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * D5 Figure 3 «Controller» OutreachController - UC09 Generate Emails via LLM (SD09), UC10 Review and Revise Drafts
 * (SD10), UC11 Dispatch Automated Outreach (SD11).
 * Associations: +runner, +workspace, +generator DraftGenerationService, +dispatcher OutreachDispatcher,
 * +drafts DraftRepository, +leads LeadRepository, «use» EmailDraft.
 */
public class OutreachController {

    private final BackgroundTaskRunner runner;
    private final Workspace workspace;
    private final DraftGenerationService generator;
    private final OutreachDispatcher dispatcher;
    private final DraftRepository drafts;
    private final LeadRepository leads;
    /** Gap G2: decrypts the SMTP password before dispatch (not drawn in D5). */
    private final CredentialEncryptor encryptor;
    /** Written by the worker thread, read by the JavaFX thread - hence volatile. */
    private volatile int lastResultCount;
    private volatile int lastFailureCount;

    public OutreachController(BackgroundTaskRunner runner, Workspace workspace, DraftGenerationService generator,
                              OutreachDispatcher dispatcher, DraftRepository drafts, LeadRepository leads,
                              CredentialEncryptor encryptor) {
        this.runner = runner;
        this.workspace = workspace;
        this.generator = generator;
        this.dispatcher = dispatcher;
        this.drafts = drafts;
        this.leads = leads;
        this.encryptor = encryptor;
    }

    /** SSD/SD09 steps 1-4: generateDrafts(qualifiedLeadIds) -> generationStarted(jobId). */
    public Long generateDrafts(List<Long> qualifiedLeadIds) {
        requireSelection(qualifiedLeadIds, "Select at least one qualified lead");
        List<Long> ids = List.copyOf(qualifiedLeadIds);
        return runner.submit(() -> runGeneration(ids));                        // SD09 steps 2-4
    }

    /** SSD/SD10 steps 1-4: openDraft(draftId) -> draftContents(subject, body). */
    public EmailDraft openDraft(Long draftId) {
        return requireDraft(drafts.findById(requireId(draftId)));             // SD10 steps 2-4
    }

    /**
     * SSD/SD10 steps 5-12: d = drafts.findById(draftId); d.revise(subject, body); d.approve(); drafts.save(d);
     * return d.getStatus(). UC10 extension: empty text -> ValidationException from EmailDraft.revise.
     */
    public DraftStatus approveRevisedDraft(Long draftId, String subject, String body) {
        EmailDraft d = requireDraft(drafts.findById(requireId(draftId)));     // SD10 steps 6-7
        d.revise(subject, body);                         // SD10 step 8 (empty text -> UC10 extension)
        d.approve();                                     // SD10 steps 9-10
        drafts.save(d);                                  // SD10 step 11
        return d.getStatus();                            // SD10 step 12
    }

    /** SSD/SD11 steps 1-4: sendApprovedEmails(leadIds) -> dispatchStarted(jobId). */
    public Long sendApprovedEmails(List<Long> leadIds) {
        requireSelection(leadIds, "Select at least one lead with an approved draft");
        List<Long> ids = List.copyOf(leadIds);
        return runner.submit(() -> runDispatch(ids));                          // SD11 steps 2-4
    }

    /**
     * SD09 steps 5-17 (background thread): list = leads.findAllByIds(ids), keep only QUALIFIED leads
     * (D3 precondition); ds = generator.generate(list, workspace); drafts.save(each d);
     * lastResultCount = ds.size(); lastFailureCount = generator.getLastFailures().
     */
    private void runGeneration(List<Long> leadIds) {
        lastResultCount = 0;
        lastFailureCount = 0;
        List<Lead> found = leads.findAllByIds(leadIds);                        // SD09 steps 6-7
        LeadFilter onlyQualified = new LeadFilter(null, PipelineStatus.QUALIFIED, null, false);
        List<Lead> qualified = new ArrayList<>();
        for (Lead lead : found) {                        // D3 UC09: "selects a batch of QUALIFIED leads"
            if (lead.matches(onlyQualified)) {           // Information Expert: the lead answers (archived excluded)
                qualified.add(lead);
            }
        }
        List<EmailDraft> ds = generator.generate(qualified, workspace);        // SD09 steps 8-15
        for (EmailDraft d : ds) {
            drafts.save(d);                                                    // SD09 step 16
        }
        lastResultCount = ds.size();                                           // SD09 step 17 draftsReady(count)
        lastFailureCount = generator.getLastFailures() + (leadIds.size() - qualified.size()); // UC09 extension
    }

    /**
     * SD11 steps 5-22 (background thread): ds = drafts.findApprovedByLeadIds(ids); smtp = workspace.getSmtpCredentials()
     * (missing host -> ValidationException); smtp = smtp.withPassword(encryptor.decrypt(smtp.password()));
     * sent = dispatcher.dispatch(ds, smtp); drafts.save(each d);
     * for each lead whose draft is SENT: lead.changeStatus(CONTACTED); leads.saveAll(...); lastResultCount = sent.
     */
    private void runDispatch(List<Long> leadIds) {
        lastResultCount = 0;
        lastFailureCount = 0;
        List<EmailDraft> ds = drafts.findApprovedByLeadIds(leadIds);           // SD11 steps 6-7
        if (ds.isEmpty()) {
            throw new ValidationException("None of the selected leads has an approved draft - approve one first (UC10)");
        }
        SmtpCredentials smtp = decrypted(workspace.getSmtpCredentials());     // SD11 steps 8-9 + gap G2
        int sent = 0;
        try {
            sent = dispatcher.dispatch(ds, smtp);                              // SD11 steps 10-17
        } finally {
            // Runs even when SMTP authentication aborts the batch: drafts already sent are saved as SENT,
            // so the next attempt never mails them twice.
            for (EmailDraft d : ds) {
                drafts.save(d);                                                // SD11 step 18
            }
            List<Long> sentLeadIds = new ArrayList<>();
            for (EmailDraft d : ds) {
                if (d.getStatus() == DraftStatus.SENT) {       // gap G14: a FAILED attempt leaves the lead unchanged
                    sentLeadIds.add(d.getLeadId());
                }
            }
            List<Lead> contacted = new ArrayList<>();
            for (Lead lead : leads.findAllByIds(sentLeadIds)) {                // SD11 steps 19-20
                try {
                    lead.changeStatus(PipelineStatus.CONTACTED);               // SD11 step 21 (State pattern)
                    contacted.add(lead);
                } catch (InvalidStateTransitionException alreadyFurther) {
                    // e.g. already CONTACTED by an earlier email: the state object decides, nothing changes
                }
            }
            leads.saveAll(contacted);                                          // SD11 step 22
        }
        lastResultCount = sent;                                                // SD11 step 23 dispatchCompleted
        lastFailureCount = ds.size() - sent;
    }

    /** NOT IN D5 - approved addition (gap G1): draftsReady generatedCount / dispatchCompleted sentCount. */
    public int getLastResultCount() {
        return lastResultCount;
    }

    /** NOT IN D5 - approved addition (gap G1): failures in the last batch (UC09/UC11 extensions). */
    public int getLastFailureCount() {
        return lastFailureCount;
    }

    // ------------------------------------------------------------ private helpers

    /**
     * Gap G2: Workspace stores the SMTP password encrypted; it is decrypted only here, in memory, for UC11.
     * No SMTP host = offline demo mode (gap G12): JakartaMailSender logs instead of sending.
     */
    private SmtpCredentials decrypted(SmtpCredentials stored) {
        if (stored.host() == null || stored.host().isBlank()) {
            return stored.withPassword(null);
        }
        if (stored.password() == null || stored.password().isBlank()) {
            throw new ValidationException("No SMTP password is saved - enter it in Workspace Settings");
        }
        try {
            return stored.withPassword(encryptor.decrypt(stored.password()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ValidationException("The saved SMTP password cannot be read - re-enter it in Workspace Settings", e);
        }
    }

    private static void requireSelection(List<Long> ids, String message) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException(message);
        }
    }

    private static Long requireId(Long draftId) {
        if (draftId == null) {
            throw new ValidationException("Select a draft first");
        }
        return draftId;
    }

    private static EmailDraft requireDraft(EmailDraft d) {
        if (d == null) {
            throw new ValidationException("Draft not found");
        }
        return d;
    }
}
