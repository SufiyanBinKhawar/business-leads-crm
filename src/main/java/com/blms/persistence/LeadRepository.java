package com.blms.persistence;

import com.blms.domain.Contact;
import com.blms.domain.DraftStatus;
import com.blms.domain.EmailDraft;
import com.blms.domain.InteractionLog;
import com.blms.domain.Lead;
import com.blms.domain.OutreachDispatch;
import com.blms.domain.PipelineStatus;
import com.blms.domain.UserAccount;
import com.blms.domain.UserRole;
import com.blms.domain.state.PipelineState;
import com.blms.dto.LeadFilter;
import com.blms.exception.ValidationException;
import com.blms.util.Validator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * D5 Figures 2-3 LeadRepository extends JdbcRepository&lt;Lead&gt;.
 * Tables: LEADS + CONTACTS + INTERACTION_LOGS (Lead composes them, so save(lead) writes all three in one
 * transaction - SD07 step 11 saves a new InteractionLog through save(lead)).
 */
public class LeadRepository extends JdbcRepository<Lead> {

    /** Lead row + its +creator (the password hash is never loaded with a lead). */
    private static final String LEAD_SELECT =
            "SELECT l.lead_id, l.company_name, l.industry, l.phone, l.status, l.is_archived, l.created_at, "
                    + "u.user_id, u.username, u.role, u.is_active "
                    + "FROM leads l JOIN users u ON u.user_id = l.created_by_user_id";

    /** Oracle allows at most 1000 expressions in an IN list. */
    private static final int IN_LIST_LIMIT = 1000;

    public LeadRepository(Connection connection) {
        super(connection);
    }

    /**
     * SD03 step 4, SD04 step 11, SD05 step 16. D3 UC03 step 4: "existing records with the same email domain".
     * SELECT COUNT(*) FROM contacts WHERE LOWER(SUBSTR(email, INSTR(email,'@')+1)) = ?
     * An invalid address is rejected here with the UC03 extension message "Invalid email format".
     */
    public boolean existsByEmailDomain(String email) {
        String domain = Validator.emailDomain(email).toLowerCase();
        return query(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM contacts WHERE LOWER(SUBSTR(email, INSTR(email, '@') + 1)) = ?")) {
                ps.setString(1, domain);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            }
        });
    }

    /** SD06 steps 3-4 and 16-17: SELECT ... WHERE built from the filter with ? placeholders only. */
    public List<Lead> findByFilter(LeadFilter f) {
        LeadFilter filter = f == null ? LeadFilter.all() : f;
        StringBuilder sql = new StringBuilder(LEAD_SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (!filter.includeArchived()) {
            sql.append(" AND l.is_archived = 0");
        }
        if (filter.industry() != null && !filter.industry().isBlank()) {
            sql.append(" AND LOWER(l.industry) = ?");
            params.add(filter.industry().trim().toLowerCase());
        }
        if (filter.status() != null) {
            sql.append(" AND l.status = ?");
            params.add(filter.status().name());
        }
        if (filter.searchText() != null && !filter.searchText().isBlank()) {
            String like = "%" + escapeLike(filter.searchText().trim().toLowerCase()) + "%";
            sql.append(" AND (LOWER(l.company_name) LIKE ? ESCAPE '\\' OR EXISTS (SELECT 1 FROM contacts c "
                    + "WHERE c.lead_id = l.lead_id AND (LOWER(c.full_name) LIKE ? ESCAPE '\\' "
                    + "OR LOWER(c.email) LIKE ? ESCAPE '\\')))");
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY l.created_at DESC, l.lead_id DESC");
        return query(() -> {
            List<Lead> found = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setString(i + 1, (String) params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        found.add(mapLead(rs));
                    }
                }
            }
            loadContacts(found);
            return found;
        });
    }

    /** SD09 step 6, SD11 step 19. Leads in the order of ids; unknown ids are skipped. */
    public List<Lead> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return query(() -> {
            Map<Long, Lead> byId = new LinkedHashMap<>();
            for (List<Long> chunk : chunks(ids.stream().filter(java.util.Objects::nonNull).distinct().toList())) {
                try (PreparedStatement ps = connection.prepareStatement(
                        LEAD_SELECT + " WHERE l.lead_id IN (" + placeholders(chunk.size()) + ")")) {
                    bindIds(ps, chunk);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Lead lead = mapLead(rs);
                            byId.put(lead.getLeadId(), lead);
                        }
                    }
                }
            }
            List<Lead> ordered = new ArrayList<>();
            for (Long id : ids) {
                Lead lead = byId.remove(id);
                if (lead != null) {
                    ordered.add(lead);
                }
            }
            loadContacts(ordered);
            return ordered;
        });
    }

    /** SD04 step 15, SD05 step 20, SD11 step 22: one transaction for the whole list. Returns rows saved. */
    public int saveAll(List<Lead> leads) {
        if (leads == null || leads.isEmpty()) {
            return 0;
        }
        List<Runnable> undo = new ArrayList<>();
        try {
            return inTransaction(() -> {
                for (Lead lead : leads) {
                    write(lead, undo);
                }
                return leads.size();
            });
        } catch (RuntimeException e) {
            undo.forEach(Runnable::run);
            throw e;
        }
    }

    /** SD08 step 19 (PipelineMetrics.recalculate). Every lead, archived included (recalculate skips those). */
    public List<Lead> findAll() {
        return query(() -> {
            List<Lead> all = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(LEAD_SELECT + " ORDER BY l.lead_id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    all.add(mapLead(rs));
                }
            }
            loadContacts(all);
            return all;
        });
    }

    /**
     * SD03 step 11, SD06 step 15, SD07 step 11, SD08 step 18.
     * inTransaction: INSERT or UPDATE leads (status = lead.getStatus().name(), is_archived 0/1);
     * INSERT/UPDATE contacts; INSERT interaction_logs whose id is still null. Never DELETE.
     * On rollback the ids handed out inside the failed transaction are cleared again (UC06 extension).
     */
    @Override
    public Lead save(Lead entity) {
        List<Runnable> undo = new ArrayList<>();
        try {
            return inTransaction(() -> write(entity, undo));
        } catch (RuntimeException e) {
            undo.forEach(Runnable::run);
            throw e;
        }
    }

    /**
     * SD06 steps 7-8: the lead with its creator, contacts, interaction history (newest first, each with its
     * author) and its email drafts (newest first, each with its +recipient and send attempts - UC10 entry point,
     * gap G14 failed attempts; CONFLICTS.md S-004). Returns null when no row exists.
     */
    @Override
    public Lead findById(Long id) {
        if (id == null) {
            return null;
        }
        return query(() -> {
            Lead lead;
            try (PreparedStatement ps = connection.prepareStatement(LEAD_SELECT + " WHERE l.lead_id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    lead = mapLead(rs);
                }
            }
            loadContacts(List.of(lead));
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT i.interaction_id, i.note, i.created_at, u.user_id, u.username, u.role, u.is_active "
                            + "FROM interaction_logs i JOIN users u ON u.user_id = i.author_user_id "
                            + "WHERE i.lead_id = ? ORDER BY i.created_at DESC, i.interaction_id DESC")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lead.addLoadedInteraction(new InteractionLog(rs.getLong("interaction_id"),
                                rs.getString("note"), ldt(rs.getTimestamp("created_at")), mapUser(rs)));
                    }
                }
            }
            loadDrafts(lead);
            return lead;
        });
    }

    // ------------------------------------------------------------------ private helpers

    /** +drafts of one lead with their +recipient (one of the lead's contacts) and +attempts. */
    private void loadDrafts(Lead lead) throws SQLException {
        Map<Long, Contact> contactsById = new LinkedHashMap<>();
        for (Contact c : lead.getContacts()) {
            contactsById.put(c.getContactId(), c);
        }
        Map<Long, EmailDraft> draftsById = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT draft_id, recipient_contact_id, subject, body, status, created_at, approved_at "
                        + "FROM email_drafts WHERE lead_id = ? ORDER BY created_at DESC, draft_id DESC")) {
            ps.setLong(1, lead.getLeadId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Contact recipient = contactsById.get(rs.getLong("recipient_contact_id"));
                    if (recipient == null) {
                        continue;                       // cannot happen: the FK points at one of the lead's contacts
                    }
                    EmailDraft d = new EmailDraft(rs.getLong("draft_id"), rs.getString("subject"),
                            rs.getString("body"), DraftStatus.valueOf(rs.getString("status")),
                            ldt(rs.getTimestamp("created_at")), ldt(rs.getTimestamp("approved_at")), recipient);
                    d.setLeadId(lead.getLeadId());
                    lead.addLoadedDraft(d);
                    draftsById.put(d.getDraftId(), d);
                }
            }
        }
        if (draftsById.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT o.dispatch_id, o.draft_id, o.sent_at, o.delivery_status, o.error_message "
                        + "FROM outreach_dispatches o JOIN email_drafts d ON d.draft_id = o.draft_id "
                        + "WHERE d.lead_id = ? ORDER BY o.sent_at, o.dispatch_id")) {
            ps.setLong(1, lead.getLeadId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmailDraft d = draftsById.get(rs.getLong("draft_id"));
                    if (d != null) {
                        d.addLoadedAttempt(new OutreachDispatch(rs.getLong("dispatch_id"),
                                ldt(rs.getTimestamp("sent_at")), rs.getString("delivery_status"),
                                rs.getString("error_message"), d.getRecipient()));
                    }
                }
            }
        }
    }

    /** Writes one lead and its parts inside the caller's transaction. */
    private Lead write(Lead lead, List<Runnable> undo) throws SQLException {
        if (lead.getCreator() == null || lead.getCreator().getUserId() == null) {
            throw new ValidationException("A lead must have a creator - please log in again");
        }
        if (lead.getLeadId() == null) {
            long id = insert("INSERT INTO leads (workspace_id, created_by_user_id, company_name, industry, phone, "
                            + "status, is_archived, created_at) "
                            + "VALUES ((SELECT MIN(workspace_id) FROM workspaces), ?, ?, ?, ?, ?, ?, ?)", "LEAD_ID",
                    ps -> {
                        ps.setLong(1, lead.getCreator().getUserId());
                        ps.setString(2, lead.getCompanyName());
                        setNullableString(ps, 3, lead.getIndustry());
                        setNullableString(ps, 4, lead.getPhone());
                        ps.setString(5, lead.getStatus().name());
                        ps.setInt(6, lead.isArchived() ? 1 : 0);
                        ps.setTimestamp(7, ts(lead.getCreatedAt()));
                    });
            lead.setLeadId(id);
            undo.add(() -> lead.setLeadId(null));
        } else {
            update("UPDATE leads SET company_name = ?, industry = ?, phone = ?, status = ?, is_archived = ? "
                    + "WHERE lead_id = ?", ps -> {
                        ps.setString(1, lead.getCompanyName());
                        setNullableString(ps, 2, lead.getIndustry());
                        setNullableString(ps, 3, lead.getPhone());
                        ps.setString(4, lead.getStatus().name());
                        ps.setInt(5, lead.isArchived() ? 1 : 0);
                        ps.setLong(6, lead.getLeadId());
                    });
        }
        for (Contact c : lead.getContacts()) {
            if (c.getContactId() == null) {
                long id = insert("INSERT INTO contacts (lead_id, full_name, email, job_role, phone) "
                        + "VALUES (?, ?, ?, ?, ?)", "CONTACT_ID", ps -> {
                            ps.setLong(1, lead.getLeadId());
                            ps.setString(2, c.getFullName());
                            ps.setString(3, c.getEmail());
                            setNullableString(ps, 4, c.getJobRole());
                            setNullableString(ps, 5, c.getPhone());
                        });
                c.setContactId(id);
                undo.add(() -> c.setContactId(null));
            } else {
                update("UPDATE contacts SET full_name = ?, email = ?, job_role = ?, phone = ? WHERE contact_id = ?",
                        ps -> {
                            ps.setString(1, c.getFullName());
                            ps.setString(2, c.getEmail());
                            setNullableString(ps, 3, c.getJobRole());
                            setNullableString(ps, 4, c.getPhone());
                            ps.setLong(5, c.getContactId());
                        });
            }
        }
        for (InteractionLog log : lead.getHistory()) {
            if (log.getInteractionId() == null) {
                long id = insert("INSERT INTO interaction_logs (lead_id, author_user_id, note, created_at) "
                        + "VALUES (?, ?, ?, ?)", "INTERACTION_ID", ps -> {
                            ps.setLong(1, lead.getLeadId());
                            ps.setLong(2, log.getAuthor().getUserId());
                            ps.setString(3, log.getNote());
                            ps.setTimestamp(4, ts(log.getCreatedAt()));
                        });
                log.setInteractionId(id);
                undo.add(() -> log.setInteractionId(null));
            }
        }
        return lead;
    }

    /** Loads the +contacts of every lead with one query per 1000 leads. */
    private void loadContacts(List<Lead> leads) throws SQLException {
        if (leads.isEmpty()) {
            return;
        }
        Map<Long, Lead> byId = new LinkedHashMap<>();
        for (Lead l : leads) {
            byId.put(l.getLeadId(), l);
        }
        for (List<Long> chunk : chunks(new ArrayList<>(byId.keySet()))) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT contact_id, lead_id, full_name, email, job_role, phone FROM contacts "
                            + "WHERE lead_id IN (" + placeholders(chunk.size()) + ") ORDER BY contact_id")) {
                bindIds(ps, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        byId.get(rs.getLong("lead_id")).addLoadedContact(new Contact(rs.getLong("contact_id"),
                                rs.getString("full_name"), rs.getString("email"), rs.getString("job_role"),
                                rs.getString("phone")));
                    }
                }
            }
        }
    }

    private static Lead mapLead(ResultSet rs) throws SQLException {
        return new Lead(rs.getLong("lead_id"), rs.getString("company_name"), rs.getString("industry"),
                rs.getString("phone"), rs.getInt("is_archived") == 1, ldt(rs.getTimestamp("created_at")),
                mapUser(rs), PipelineState.forStatus(PipelineStatus.valueOf(rs.getString("status"))));
    }

    /** Creator / author: identity and role only - the password hash stays in USERS. */
    private static UserAccount mapUser(ResultSet rs) throws SQLException {
        return new UserAccount(rs.getLong("user_id"), rs.getString("username"), null,
                UserRole.valueOf(rs.getString("role")), rs.getInt("is_active") == 1);
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value.trim());
        }
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String placeholders(int n) {
        return String.join(", ", Collections.nCopies(n, "?"));
    }

    private static void bindIds(PreparedStatement ps, List<Long> ids) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            ps.setLong(i + 1, ids.get(i));
        }
    }

    private static List<List<Long>> chunks(List<Long> ids) {
        List<List<Long>> out = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += IN_LIST_LIMIT) {
            out.add(ids.subList(i, Math.min(ids.size(), i + IN_LIST_LIMIT)));
        }
        return out;
    }
}
