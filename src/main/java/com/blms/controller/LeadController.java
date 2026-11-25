package com.blms.controller;

import com.blms.domain.InteractionLog;
import com.blms.domain.Lead;
import com.blms.domain.PipelineMetrics;
import com.blms.domain.PipelineStatus;
import com.blms.domain.Session;
import com.blms.domain.UserAccount;
import com.blms.domain.Workspace;
import com.blms.dto.LeadData;
import com.blms.dto.LeadFilter;
import com.blms.dto.LeadForm;
import com.blms.dto.NoteForm;
import com.blms.exception.AuthenticationException;
import com.blms.exception.DuplicateLeadException;
import com.blms.exception.ValidationException;
import com.blms.persistence.LeadRepository;
import com.blms.util.Validator;

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
        UserAccount user = currentUser();
        if (leads.existsByEmailDomain(email)) {                                        // SD03 steps 4-5
            throw new DuplicateLeadException("This lead already exists - a lead with the email domain '"
                    + Validator.emailDomain(email) + "' is already in the CRM");          // UC03 extension
        }
        Lead lead = workspace.createLead(
                new LeadData(companyName, contactName, email, jobRole, null, null, user)); // SD03 steps 6-10
        return leads.save(lead);                                                         // SD03 steps 11-12
    }

    // ------------------------------------------------------------ UC06 (SD06)

    /** SD06 steps 1-5: currentFilter = criteria; return leads.findByFilter(criteria). */
    public List<Lead> filterLeads(LeadFilter criteria) {
        currentFilter = criteria == null ? LeadFilter.all() : criteria;                 // SD06 step 2
        return leads.findByFilter(currentFilter);                                        // SD06 steps 3-4
    }

    /** SD06 steps 6-9: leads.findById(leadId). */
    public Lead editLead(Long leadId) {
        return requireLead(leads.findById(requireId(leadId)));                           // SD06 steps 7-8
    }

    /**
     * SD06 steps 10-18: lead.applyUpdates(updates); if archived -> lead.archive() (soft delete);
     * leads.save(lead) (rolls back on failure); return leads.findByFilter(currentFilter).
     */
    public List<Lead> saveLeadChanges(Long leadId, LeadData updates, boolean archived) {
        if (updates == null) {
            throw new ValidationException("Lead details are required");
        }
        Lead lead = requireLead(leads.findById(requireId(leadId)));                    // SD06 steps 11-12
        lead.applyUpdates(updates);                                                      // SD06 step 13
        if (archived) {
            lead.archive();                                                              // SD06 step 14 (opt)
        }
        leads.save(lead);                                       // SD06 step 15 - rolls back on failure (UC06 ext.)
        return leads.findByFilter(currentFilter);                                        // SD06 steps 16-17
    }

    // ------------------------------------------------------------ UC07 (SD07)

    /** SD07 steps 1-2: addInteractionNote(leadId) -> noteInputForm(). */
    public NoteForm addInteractionNote(Long leadId) {
        return new NoteForm(requireId(leadId), null);                                    // SD07 steps 1-2
    }

    /**
     * SD07 steps 3-12: user = session.get().getUser(); lead = leads.findById(leadId);
     * log = lead.addInteraction(note, user) (Creator); leads.save(lead); return log.
     */
    public InteractionLog submitInteraction(Long leadId, String note) {
        UserAccount user = currentUser();                                                // SD07 steps 4-5
        Lead lead = requireLead(leads.findById(requireId(leadId)));                    // SD07 steps 6-7
        InteractionLog log = lead.addInteraction(note, user);  // SD07 steps 8-10 (empty note -> UC07 extension)
        leads.save(lead);                                                                // SD07 step 11
        return log;                                                                      // SD07 step 12
    }

    // ------------------------------------------------------------ UC08 (SD08)

    /**
     * NOT IN D5 - approved addition (gap G10): the dashboard needs the counts before any status change.
     * metrics.recalculate(leads.findAll()); return metrics.
     */
    public PipelineMetrics getPipelineMetrics() {
        metrics.recalculate(leads.findAll());
        return metrics;
    }

    /** SD08 steps 1-8: leads.findById(leadId).getStatus(). */
    public PipelineStatus selectLead(Long leadId) {
        Lead lead = requireLead(leads.findById(requireId(leadId)));                    // SD08 steps 2-3
        return lead.getStatus();                                                         // SD08 steps 4-7
    }

    /**
     * SD08 steps 9-22: lead.changeStatus(newStatus) (InvalidStateTransitionException = UC08 extension);
     * leads.save(lead); metrics.recalculate(leads.findAll()); return metrics.
     */
    public PipelineMetrics changePipelineStatus(Long leadId, PipelineStatus newStatus) {
        if (newStatus == null) {
            throw new ValidationException("Choose a pipeline status");
        }
        Lead lead = requireLead(leads.findById(requireId(leadId)));                    // SD08 steps 10-11
        lead.changeStatus(newStatus);    // SD08 steps 12-17 (InvalidStateTransitionException = UC08 extension)
        leads.save(lead);                                                                // SD08 step 18
        metrics.recalculate(leads.findAll());                                            // SD08 steps 19-21
        return metrics;                                                                  // SD08 step 22
    }

    // ------------------------------------------------------------ private helpers

    /** The +session user (gap G6): the Session exists only after UC01. */
    private UserAccount currentUser() {
        Session current = session.get();
        if (current == null) {
            throw new AuthenticationException("Please log in first");
        }
        return current.getUser();
    }

    private static Long requireId(Long leadId) {
        if (leadId == null) {
            throw new ValidationException("Select a lead first");
        }
        return leadId;
    }

    private static Lead requireLead(Lead lead) {
        if (lead == null) {
            throw new ValidationException("Lead not found - it may have been removed");
        }
        return lead;
    }
}
