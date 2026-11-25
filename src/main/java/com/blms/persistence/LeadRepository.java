package com.blms.persistence;

import com.blms.domain.Lead;
import com.blms.dto.LeadFilter;

import java.sql.Connection;
import java.util.List;

/**
 * D5 Figures 2-3 LeadRepository extends JdbcRepository&lt;Lead&gt;.
 * Tables: LEADS + CONTACTS + INTERACTION_LOGS (Lead composes them, so save(lead) writes all three in one
 * transaction - SD07 step 11 saves a new InteractionLog through save(lead)).
 */
public class LeadRepository extends JdbcRepository<Lead> {

    public LeadRepository(Connection connection) {
        super(connection);
    }

    /**
     * SD03 step 4, SD04 step 11, SD05 step 16. D3 UC03 step 4: "existing records with the same email domain".
     * SELECT COUNT(*) FROM contacts WHERE LOWER(SUBSTR(email, INSTR(email,'@')+1)) = ?
     */
    public boolean existsByEmailDomain(String email) {
        // TODO(Sufiyan, UC03)
        throw new UnsupportedOperationException("TODO UC03 LeadRepository.existsByEmailDomain");
    }

    /** SD06 steps 3-4 and 16-17: SELECT ... WHERE built from the filter with ? placeholders only. */
    public List<Lead> findByFilter(LeadFilter f) {
        // TODO(Sufiyan, UC06): WHERE is_archived = 0 (unless f.includeArchived()) AND industry = ? AND status = ?
        //  AND (LOWER(company_name) LIKE ? ...); load contacts for each lead
        throw new UnsupportedOperationException("TODO UC06 LeadRepository.findByFilter");
    }

    /** SD09 step 6, SD11 step 19. */
    public List<Lead> findAllByIds(List<Long> ids) {
        // TODO(Sufiyan, UC09/UC11)
        throw new UnsupportedOperationException("TODO LeadRepository.findAllByIds");
    }

    /** SD04 step 15, SD05 step 20, SD11 step 22: one transaction for the whole list. Returns rows saved. */
    public int saveAll(List<Lead> leads) {
        // TODO(Sufiyan, UC04/UC05/UC11): inTransaction(() -> { for each lead: saveLead(lead) } )
        throw new UnsupportedOperationException("TODO LeadRepository.saveAll");
    }

    /** SD08 step 19 (PipelineMetrics.recalculate). */
    public List<Lead> findAll() {
        // TODO(Sufiyan, UC08)
        throw new UnsupportedOperationException("TODO UC08 LeadRepository.findAll");
    }

    /**
     * SD03 step 11, SD06 step 15, SD07 step 11, SD08 step 18.
     * inTransaction: INSERT or UPDATE leads (status = lead.getStatus().name(), is_archived 0/1);
     * INSERT/UPDATE contacts; INSERT interaction_logs whose id is still null. Never DELETE.
     */
    @Override
    public Lead save(Lead entity) {
        // TODO(Sufiyan, UC03/UC06/UC07/UC08)
        throw new UnsupportedOperationException("TODO LeadRepository.save");
    }

    /** SD06 steps 7-8: the lead with contacts, interaction history (newest first), drafts (for UC10) and creator. */
    @Override
    public Lead findById(Long id) {
        // TODO(Sufiyan, UC06/UC07/UC08): state = PipelineState.forStatus(PipelineStatus.valueOf(status))
        throw new UnsupportedOperationException("TODO LeadRepository.findById");
    }
}
