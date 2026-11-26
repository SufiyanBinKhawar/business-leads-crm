package com.blms.persistence;

import com.blms.domain.Contact;
import com.blms.domain.DraftStatus;
import com.blms.domain.EmailDraft;
import com.blms.domain.OutreachDispatch;
import com.blms.exception.ValidationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * D5 Figure 3 DraftRepository extends JdbcRepository&lt;EmailDraft&gt;.
 * Tables: EMAIL_DRAFTS + OUTREACH_DISPATCHES (EmailDraft composes its attempts, so save(d) writes new attempts too).
 */
public class DraftRepository extends JdbcRepository<EmailDraft> {

    /** Draft row + its +recipient Contact. */
    private static final String DRAFT_SELECT =
            "SELECT d.draft_id, d.lead_id, d.subject, d.body, d.status, d.created_at, d.approved_at, "
                    + "c.contact_id, c.full_name, c.email, c.job_role, c.phone "
                    + "FROM email_drafts d JOIN contacts c ON c.contact_id = d.recipient_contact_id";

    private static final int IN_LIST_LIMIT = 1000;

    public DraftRepository(Connection connection) {
        super(connection);
    }

    /**
     * SD11 steps 6-7: APPROVED drafts of the given leads, with +recipient Contact loaded.
     * One draft per lead - the most recently approved - so a contact never receives two emails in one batch.
     */
    public List<EmailDraft> findApprovedByLeadIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        return query(() -> {
            List<EmailDraft> found = new ArrayList<>();
            for (int from = 0; from < clean.size(); from += IN_LIST_LIMIT) {
                List<Long> chunk = clean.subList(from, Math.min(clean.size(), from + IN_LIST_LIMIT));
                String sql = "SELECT * FROM ("
                        + "SELECT d.draft_id, d.lead_id, d.subject, d.body, d.status, d.created_at, d.approved_at, "
                        + "c.contact_id, c.full_name, c.email, c.job_role, c.phone, "
                        + "ROW_NUMBER() OVER (PARTITION BY d.lead_id ORDER BY d.approved_at DESC, d.draft_id DESC) rn "
                        + "FROM email_drafts d JOIN contacts c ON c.contact_id = d.recipient_contact_id "
                        + "WHERE d.status = ? AND d.lead_id IN ("
                        + String.join(", ", Collections.nCopies(chunk.size(), "?"))
                        + ")) WHERE rn = 1 ORDER BY lead_id";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, DraftStatus.APPROVED.name());
                    for (int i = 0; i < chunk.size(); i++) {
                        ps.setLong(i + 2, chunk.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            found.add(map(rs));
                        }
                    }
                }
            }
            return found;
        });
    }

    /**
     * SD09 step 16, SD10 step 11, SD11 step 18. INSERT/UPDATE email_drafts; INSERT attempts with null id.
     * Ids handed out inside a rolled-back transaction are cleared again.
     */
    @Override
    public EmailDraft save(EmailDraft entity) {
        if (entity.getLeadId() == null || entity.getRecipient() == null || entity.getRecipient().getContactId() == null) {
            throw new ValidationException("A draft must belong to a saved lead and contact");
        }
        List<Runnable> undo = new ArrayList<>();
        try {
            return inTransaction(() -> {
                write(entity, undo);
                return entity;
            });
        } catch (RuntimeException e) {
            undo.forEach(Runnable::run);
            throw e;
        }
    }

    /** SD10 steps 2-3. Returns null when no row exists. */
    @Override
    public EmailDraft findById(Long id) {
        if (id == null) {
            return null;
        }
        return query(() -> {
            try (PreparedStatement ps = connection.prepareStatement(DRAFT_SELECT + " WHERE d.draft_id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    // ------------------------------------------------------------------ private helpers

    private void write(EmailDraft d, List<Runnable> undo) throws SQLException {
        if (d.getDraftId() == null) {
            long id = insert("INSERT INTO email_drafts (lead_id, recipient_contact_id, subject, body, status, "
                    + "created_at, approved_at) VALUES (?, ?, ?, ?, ?, ?, ?)", "DRAFT_ID", ps -> {
                        ps.setLong(1, d.getLeadId());
                        ps.setLong(2, d.getRecipient().getContactId());
                        ps.setString(3, d.getSubject());
                        ps.setString(4, d.getBody());
                        ps.setString(5, d.getStatus().name());
                        ps.setTimestamp(6, ts(d.getCreatedAt()));
                        setTimestampOrNull(ps, 7, d);
                    });
            d.setDraftId(id);
            undo.add(() -> d.setDraftId(null));
        } else {
            update("UPDATE email_drafts SET subject = ?, body = ?, status = ?, approved_at = ? WHERE draft_id = ?",
                    ps -> {
                        ps.setString(1, d.getSubject());
                        ps.setString(2, d.getBody());
                        ps.setString(3, d.getStatus().name());
                        setTimestampOrNull(ps, 4, d);
                        ps.setLong(5, d.getDraftId());
                    });
        }
        for (OutreachDispatch a : d.getAttempts()) {
            if (a.getDispatchId() == null) {
                long id = insert("INSERT INTO outreach_dispatches (draft_id, recipient_contact_id, sent_at, "
                        + "delivery_status, error_message) VALUES (?, ?, ?, ?, ?)", "DISPATCH_ID", ps -> {
                            ps.setLong(1, d.getDraftId());
                            ps.setLong(2, a.getRecipient().getContactId());
                            ps.setTimestamp(3, ts(a.getSentAt()));
                            ps.setString(4, a.getDeliveryStatus());
                            String err = a.getErrorMessage();
                            ps.setString(5, err == null || err.length() <= 1000 ? err : err.substring(0, 1000));
                        });
                a.setDispatchId(id);
                undo.add(() -> a.setDispatchId(null));
            }
        }
    }

    private static void setTimestampOrNull(PreparedStatement ps, int index, EmailDraft d) throws SQLException {
        if (d.getApprovedAt() == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, ts(d.getApprovedAt()));
        }
    }

    private static EmailDraft map(ResultSet rs) throws SQLException {
        Contact recipient = new Contact(rs.getLong("contact_id"), rs.getString("full_name"), rs.getString("email"),
                rs.getString("job_role"), rs.getString("phone"));
        EmailDraft d = new EmailDraft(rs.getLong("draft_id"), rs.getString("subject"), rs.getString("body"),
                DraftStatus.valueOf(rs.getString("status")), ldt(rs.getTimestamp("created_at")),
                ldt(rs.getTimestamp("approved_at")), recipient);
        d.setLeadId(rs.getLong("lead_id"));
        return d;
    }
}
