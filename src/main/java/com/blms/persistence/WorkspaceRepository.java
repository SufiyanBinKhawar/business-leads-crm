package com.blms.persistence;

import com.blms.domain.IdealCustomerProfile;
import com.blms.domain.Workspace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        return query(() -> {
            Workspace ws;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM workspaces ORDER BY workspace_id FETCH FIRST 1 ROWS ONLY");
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                ws = map(rs);
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

    private static Workspace map(ResultSet rs) throws SQLException {
        return new Workspace(rs.getLong("workspace_id"), rs.getString("company_name"), rs.getString("target_industry"),
                rs.getString("services_offered"), rs.getString("smtp_host"), rs.getInt("smtp_port"),
                rs.getString("smtp_username"), rs.getString("smtp_credential_encrypted"));
    }

    /** SD02 step 13 / SD05 (ApiCallLog). inTransaction: UPDATE workspaces; MERGE icp; INSERT api logs with null id. */
    @Override
    public Workspace save(Workspace entity) {
        // TODO(Sufiyan, UC02/UC05)
        throw new UnsupportedOperationException("TODO UC02 WorkspaceRepository.save");
    }

    @Override
    public Workspace findById(Long id) {
        // TODO(Sufiyan)
        throw new UnsupportedOperationException("TODO WorkspaceRepository.findById");
    }
}
