package com.blms.controller;

import com.blms.domain.InteractionLog;
import com.blms.domain.Lead;
import com.blms.domain.PipelineMetrics;
import com.blms.domain.PipelineStatus;
import com.blms.domain.Session;
import com.blms.domain.Workspace;
import com.blms.dto.LeadData;
import com.blms.dto.LeadFilter;
import com.blms.dto.LeadForm;
import com.blms.dto.NoteForm;
import com.blms.persistence.LeadRepository;

import java.util.List;
import java.util.function.Supplier;

/**
 * D5 Figure 2 «Controller» LeadController - UC03, UC06, UC07, UC08 (SD03, SD06, SD07, SD08).
 * Attributes: - session : Session, - currentFilter : LeadFilter.
 * Associations: +session 1 Session, +leads 1 LeadRepository, +metrics 1 PipelineMetrics, +workspace 1 Workspace.
 */
public class LeadController {

    /** D5 "- session : Session"; supplied lazily because the Session only exists after UC01 (gap G6). */
    private final Supplier<Session> session;
    private LeadFilter currentFilter = LeadFilter.all();
    private final LeadRepository leads;
    private final PipelineMetrics metrics;
    private final Workspace workspace;

    public LeadController(Supplier<Session> session, LeadRepository leads, PipelineMetrics metrics,
                          Workspace workspace) {
        this.session = session;
        this.leads = leads;
        this.metrics = metrics;
        this.workspace = workspace;
    }

    // ------------------------------------------------------------ UC03 (SD03)

    /** SD03 steps 1-2: requestNewLeadForm() -> blankLeadForm(). */
    public LeadForm requestNewLeadForm() {
        return LeadForm.blank();
    }

    /**
     * SD03 steps 3-13: saveLead(companyName, contactName, email, jobRole) -> leadCreated(leadId, Discovered).
     * 4-5. leads.existsByEmailDomain(email) -> true: DuplicateLeadException (UC03 extension)
     * 6-10. lead = workspace.createLead(new LeadData(..., session.get().getUser()))   (Creator chain)
     * 11-12. leads.save(lead)  (Validator rejects an invalid email - UC03 extension)
     */
    public Lead saveLead(String companyName, String contactName, String email, String jobRole) {
        // TODO(Sufiyan, UC03)
        throw new UnsupportedOperationException("TODO UC03 saveLead");
    }

    // ------------------------------------------------------------ UC06 (SD06)

    /** SD06 steps 1-5: currentFilter = criteria; return leads.findByFilter(criteria). */
    public List<Lead> filterLeads(LeadFilter criteria) {
        // TODO(Sufiyan, UC06)
        throw new UnsupportedOperationException("TODO UC06 filterLeads");
    }

    /** SD06 steps 6-9: leads.findById(leadId). */
    public Lead editLead(Long leadId) {
        // TODO(Sufiyan, UC06)
        throw new UnsupportedOperationException("TODO UC06 editLead");
    }

    /**
     * SD06 steps 10-18: lead.applyUpdates(updates); if archived -> lead.archive() (soft delete);
     * leads.save(lead) (rolls back on failure); return leads.findByFilter(currentFilter).
     */
    public List<Lead> saveLeadChanges(Long leadId, LeadData updates, boolean archived) {
        // TODO(Sufiyan, UC06)
        throw new UnsupportedOperationException("TODO UC06 saveLeadChanges");
    }

    // ------------------------------------------------------------ UC07 (SD07)

    /** SD07 steps 1-2: addInteractionNote(leadId) -> noteInputForm(). */
    public NoteForm addInteractionNote(Long leadId) {
        // TODO(Sufiyan, UC07): lead = leads.findById(leadId); return new NoteForm(leadId, lead.getCompanyName())
        throw new UnsupportedOperationException("TODO UC07 addInteractionNote");
    }

    /**
     * SD07 steps 3-12: user = session.get().getUser(); lead = leads.findById(leadId);
     * log = lead.addInteraction(note, user) (Creator); leads.save(lead); return log.
     */
    public InteractionLog submitInteraction(Long leadId, String note) {
        // TODO(Sufiyan, UC07)
        throw new UnsupportedOperationException("TODO UC07 submitInteraction");
    }

    // ------------------------------------------------------------ UC08 (SD08)

    /**
     * NOT IN D5 - approved addition (gap G10): the dashboard needs the counts before any status change.
     * metrics.recalculate(leads.findAll()); return metrics.
     */
    public PipelineMetrics getPipelineMetrics() {
        // TODO(Sufiyan, UC08)
        throw new UnsupportedOperationException("TODO UC08 getPipelineMetrics");
    }

    /** SD08 steps 1-8: leads.findById(leadId).getStatus(). */
    public PipelineStatus selectLead(Long leadId) {
        // TODO(Sufiyan, UC08)
        throw new UnsupportedOperationException("TODO UC08 selectLead");
    }

    /**
     * SD08 steps 9-22: lead.changeStatus(newStatus) (InvalidStateTransitionException = UC08 extension);
     * leads.save(lead); metrics.recalculate(leads.findAll()); return metrics.
     */
    public PipelineMetrics changePipelineStatus(Long leadId, PipelineStatus newStatus) {
        // TODO(Sufiyan, UC08)
        throw new UnsupportedOperationException("TODO UC08 changePipelineStatus");
    }
}
