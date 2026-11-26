package com.blms.persistence;

import com.blms.domain.ApiCallLog;
import com.blms.domain.IdealCustomerProfile;
import com.blms.domain.Workspace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * D5 Figure 2 WorkspaceRepository extends JdbcRepository&lt;Workspace&gt;.
 * Tables: WORKSPACES + IDEAL_CUSTOMER_PROFILES + API_CALL_LOGS (Workspace composes/aggregates them, so saving the
 * workspace saves its ICP and any new ApiCallLogs in the same transaction).
 */
public class WorkspaceRepository extends JdbcRepository<Workspace> {

    public WorkspaceRepository(Connection connection) {
        super(connection);
    }

    /**
     * SD02 steps 2-3. The installation's workspace (the row with the lowest workspace_id), with its ICP loaded.
     * (D1 "multi-profile" is reduced to one active workspace by D5 - CONFLICTS.md gap G9.)
     */
    public Workspace findCurrent() {
        return load("SELECT * FROM workspaces ORDER BY workspace_id FETCH FIRST 1 ROWS ONLY", null);
    }

    private static Workspace map(ResultSet rs) throws SQLException {
        return new Workspace(rs.getLong("workspace_id"), rs.getString("company_name"), rs.getString("target_industry"),
                rs.getString("services_offered"), rs.getString("smtp_host"), rs.getInt("smtp_port"),
                rs.getString("smtp_username"), rs.getString("smtp_credential_encrypted"));
    }

    /**
     * SD02 step 13 / SD05 (ApiCallLog, gap G4). inTransaction: UPDATE (or INSERT) workspaces; the ICP row is
     * updated in place (one ICP per workspace - workspace_id is UNIQUE) or inserted; API logs with a null id are
     * inserted. Ids handed out inside a rolled-back transaction are cleared again.
     */
    @Override
    public Workspace save(Workspace entity) {
        List<Runnable> undo = new ArrayList<>();
        try {
            return inTransaction(() -> {
                writeWorkspace(entity, undo);
                writeIcp(entity, undo);
                for (ApiCallLog log : entity.getApiLogs()) {
                    if (log.getCallId() == null) {
                        long id = insert("INSERT INTO api_call_logs (workspace_id, provider_name, operation, "
                                + "status_code, error_message, occurred_at) VALUES (?, ?, ?, ?, ?, ?)", "CALL_ID", ps -> {
                                    ps.setLong(1, entity.getWorkspaceId());
                                    ps.setString(2, log.getProviderName());
                                    ps.setString(3, log.getOperation());
                                    ps.setInt(4, log.getStatusCode());
                                    ps.setString(5, truncate(log.getErrorMessage(), 1000));
                                    ps.setTimestamp(6, ts(log.getOccurredAt()));
                                });
                        log.setCallId(id);
                        undo.add(() -> log.setCallId(null));
                    }
                }
                return entity;
            });
        } catch (RuntimeException e) {
            undo.forEach(Runnable::run);
            throw e;
        }
    }

    /** The workspace with this id and its ICP; null when no row exists. */
    @Override
    public Workspace findById(Long id) {
        if (id == null) {
            return null;
        }
        return load("SELECT * FROM workspaces WHERE workspace_id = ?", id);
    }

    // ------------------------------------------------------------------ private helpers

    private Workspace load(String sql, Long id) {
        return query(() -> {
            Workspace ws;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                if (id != null) {
                    ps.setLong(1, id);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    ws = map(rs);
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT icp_id, target_industry, criteria, generated_at FROM ideal_customer_profiles "
                            + "WHERE workspace_id = ?")) {
                ps.setLong(1, ws.getWorkspaceId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ws.setLoadedIcp(new IdealCustomerProfile(rs.getLong("icp_id"), rs.getString("target_industry"),
                                rs.getString("criteria"), ldt(rs.getTimestamp("generated_at"))));
                    }
                }
            }
            return ws;
        });
    }

    private void writeWorkspace(Workspace ws, List<Runnable> undo) throws SQLException {
        Binder fields = ps -> {
            ps.setString(1, ws.getCompanyName());
            ps.setString(2, ws.getTargetIndustry());
            ps.setString(3, ws.getServicesOffered());
            ps.setString(4, ws.getSmtpHost());
            ps.setInt(5, ws.getSmtpPort());
            ps.setString(6, ws.getSmtpUsername());
            ps.setString(7, ws.getSmtpCredentialEncrypted());
        };
        if (ws.getWorkspaceId() == null) {
            long id = insert("INSERT INTO workspaces (company_name, target_industry, services_offered, smtp_host, "
                    + "smtp_port, smtp_username, smtp_credential_encrypted) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    "WORKSPACE_ID", fields);
            ws.setWorkspaceId(id);
            undo.add(() -> ws.setWorkspaceId(null));
        } else {
            update("UPDATE workspaces SET company_name = ?, target_industry = ?, services_offered = ?, smtp_host = ?, "
                    + "smtp_port = ?, smtp_username = ?, smtp_credential_encrypted = ? WHERE workspace_id = ?", ps -> {
                        fields.bind(ps);
                        ps.setLong(8, ws.getWorkspaceId());
                    });
        }
    }

    private void writeIcp(Workspace ws, List<Runnable> undo) throws SQLException {
        IdealCustomerProfile icp = ws.getIcp();
        if (icp == null) {
            return;
        }
        int updated = update("UPDATE ideal_customer_profiles SET target_industry = ?, criteria = ?, generated_at = ? "
                + "WHERE workspace_id = ?", ps -> {
                    ps.setString(1, icp.getTargetIndustry());
                    ps.setString(2, icp.getCriteria());
                    ps.setTimestamp(3, ts(icp.getGeneratedAt()));
                    ps.setLong(4, ws.getWorkspaceId());
                });
        Long previous = icp.getIcpId();
        if (updated == 0) {
            icp.setIcpId(insert("INSERT INTO ideal_customer_profiles (workspace_id, target_industry, criteria, "
                    + "generated_at) VALUES (?, ?, ?, ?)", "ICP_ID", ps -> {
                        ps.setLong(1, ws.getWorkspaceId());
                        ps.setString(2, icp.getTargetIndustry());
                        ps.setString(3, icp.getCriteria());
                        ps.setTimestamp(4, ts(icp.getGeneratedAt()));
                    }));
        } else {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT icp_id FROM ideal_customer_profiles WHERE workspace_id = ?")) {
                ps.setLong(1, ws.getWorkspaceId());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    icp.setIcpId(rs.getLong(1));
                }
            }
        }
        undo.add(() -> icp.setIcpId(previous));
    }

    private static String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
