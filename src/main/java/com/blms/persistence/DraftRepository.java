package com.blms.persistence;

import com.blms.domain.EmailDraft;

import java.sql.Connection;
import java.util.List;

/**
 * D5 Figure 3 DraftRepository extends JdbcRepository&lt;EmailDraft&gt;.
 * Tables: EMAIL_DRAFTS + OUTREACH_DISPATCHES (EmailDraft composes its attempts, so save(d) writes new attempts too).
 */
public class DraftRepository extends JdbcRepository<EmailDraft> {

    public DraftRepository(Connection connection) {
        super(connection);
    }

    /** SD11 steps 6-7: APPROVED drafts of the given leads, with +recipient Contact loaded. */
    public List<EmailDraft> findApprovedByLeadIds(List<Long> ids) {
        // TODO(Sufiyan, UC11)
        throw new UnsupportedOperationException("TODO UC11 DraftRepository.findApprovedByLeadIds");
    }

    /** SD09 step 16, SD10 step 11, SD11 step 18. INSERT/UPDATE email_drafts; INSERT attempts with null id. */
    @Override
    public EmailDraft save(EmailDraft entity) {
        // TODO(Sufiyan, UC09/UC10/UC11)
        throw new UnsupportedOperationException("TODO DraftRepository.save");
    }

    /** SD10 steps 2-3. */
    @Override
    public EmailDraft findById(Long id) {
        // TODO(Sufiyan, UC10)
        throw new UnsupportedOperationException("TODO UC10 DraftRepository.findById");
    }
}
