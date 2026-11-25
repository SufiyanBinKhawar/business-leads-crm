package com.blms.controller;

import com.blms.domain.DraftStatus;
import com.blms.domain.EmailDraft;
import com.blms.domain.Workspace;
import com.blms.persistence.DraftRepository;
import com.blms.persistence.LeadRepository;
import com.blms.security.CredentialEncryptor;
import com.blms.service.BackgroundTaskRunner;
import com.blms.service.DraftGenerationService;
import com.blms.service.OutreachDispatcher;

import java.util.List;

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
    private int lastResultCount;
    private int lastFailureCount;

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
        return runner.submit(() -> runGeneration(qualifiedLeadIds));
    }

    /** SSD/SD10 steps 1-4: openDraft(draftId) -> draftContents(subject, body). */
    public EmailDraft openDraft(Long draftId) {
        // TODO(Sufiyan, UC10): drafts.findById(draftId)
        throw new UnsupportedOperationException("TODO UC10 openDraft");
    }

    /**
     * SSD/SD10 steps 5-12: d = drafts.findById(draftId); d.revise(subject, body); d.approve(); drafts.save(d);
     * return d.getStatus(). UC10 extension: empty text -> ValidationException from EmailDraft.revise.
     */
    public DraftStatus approveRevisedDraft(Long draftId, String subject, String body) {
        // TODO(Sufiyan, UC10)
        throw new UnsupportedOperationException("TODO UC10 approveRevisedDraft");
    }

    /** SSD/SD11 steps 1-4: sendApprovedEmails(leadIds) -> dispatchStarted(jobId). */
    public Long sendApprovedEmails(List<Long> leadIds) {
        return runner.submit(() -> runDispatch(leadIds));
    }

    /**
     * SD09 steps 5-17 (background thread): list = leads.findAllByIds(ids), keep only QUALIFIED leads
     * (D3 precondition); ds = generator.generate(list, workspace); drafts.save(each d);
     * lastResultCount = ds.size(); lastFailureCount = generator.getLastFailures().
     */
    private void runGeneration(List<Long> leadIds) {
        // TODO(Sufiyan, UC09)
        throw new UnsupportedOperationException("TODO UC09 runGeneration");
    }

    /**
     * SD11 steps 5-22 (background thread): ds = drafts.findApprovedByLeadIds(ids); smtp = workspace.getSmtpCredentials()
     * (missing host -> ValidationException); smtp = smtp.withPassword(encryptor.decrypt(smtp.password()));
     * sent = dispatcher.dispatch(ds, smtp); drafts.save(each d);
     * for each lead whose draft is SENT: lead.changeStatus(CONTACTED); leads.saveAll(...); lastResultCount = sent.
     */
    private void runDispatch(List<Long> leadIds) {
        // TODO(Sufiyan, UC11)
        throw new UnsupportedOperationException("TODO UC11 runDispatch");
    }

    /** NOT IN D5 - approved addition (gap G1): draftsReady generatedCount / dispatchCompleted sentCount. */
    public int getLastResultCount() {
        return lastResultCount;
    }

    /** NOT IN D5 - approved addition (gap G1): failures in the last batch (UC09/UC11 extensions). */
    public int getLastFailureCount() {
        return lastFailureCount;
    }
}
