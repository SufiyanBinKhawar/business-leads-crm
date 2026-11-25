package com.blms.persistence;

import com.blms.domain.UserAccount;
import com.blms.domain.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * D5 Figure 2 UserRepository extends JdbcRepository&lt;UserAccount&gt; (+users of LoginController).
 * REFERENCE IMPLEMENTATION - every other repository follows this style. Table: USERS.
 */
public class UserRepository extends JdbcRepository<UserAccount> {

    private static final String COLUMNS = "user_id, username, password_hash, role, is_active";

    public UserRepository(Connection connection) {
        super(connection);
    }

    /** SD01 steps 2-3. Returns null when the username does not exist. */
    public UserAccount findByUsername(String username) {
        return query(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    @Override
    public UserAccount findById(Long id) {
        return query(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM users WHERE user_id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    /** Inserts a new user into the first workspace (no user-management use case exists in D3). */
    @Override
    public UserAccount save(UserAccount user) {
        return inTransaction(() -> {
            if (user.getUserId() == null) {
                long id = insert("INSERT INTO users (workspace_id, username, password_hash, role, is_active) "
                                + "VALUES ((SELECT MIN(workspace_id) FROM workspaces), ?, ?, ?, ?)", "USER_ID",
                        ps -> {
                            ps.setString(1, user.getUsername());
                            ps.setString(2, user.getPasswordHash());
                            ps.setString(3, user.getRole().name());
                            ps.setInt(4, user.isActive() ? 1 : 0);
                        });
                user.setUserId(id);
            } else {
                update("UPDATE users SET password_hash = ?, role = ?, is_active = ? WHERE user_id = ?", ps -> {
                    ps.setString(1, user.getPasswordHash());
                    ps.setString(2, user.getRole().name());
                    ps.setInt(3, user.isActive() ? 1 : 0);
                    ps.setLong(4, user.getUserId());
                });
            }
            return user;
        });
    }

    private static UserAccount map(ResultSet rs) throws SQLException {
        return new UserAccount(rs.getLong("user_id"), rs.getString("username"), rs.getString("password_hash"),
                UserRole.valueOf(rs.getString("role")), rs.getInt("is_active") == 1);
    }
}
