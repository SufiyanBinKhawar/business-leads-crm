package com.blms.persistence;

import com.blms.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * D5 Figures 2-3 «Pure Fabrication» abstract JdbcRepository&lt;T&gt; (# connection : Connection,
 * + save(entity : T) : T, + findById(id : Long) : T). Parent of UserRepository, WorkspaceRepository,
 * LeadRepository and DraftRepository. All SQL in the system lives in these classes (Oracle, PreparedStatement only).
 * The protected helpers below are implementation details (not public API, so not drawn in D5).
 */
public abstract class JdbcRepository<T> {

    protected final Connection connection;

    protected JdbcRepository(Connection connection) {
        this.connection = connection;
    }

    /** INSERT when the entity has no id yet, UPDATE otherwise. Returns the same entity with its id set. */
    public abstract T save(T entity);

    /** Returns null when no row exists. */
    public abstract T findById(Long id);

    // ------------------------------------------------------------------ helpers for subclasses

    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    protected interface SqlWork<R> {
        R run() throws SQLException;
    }

    /** INSERT and return the Oracle identity value of idColumn. */
    protected long insert(String sql, String idColumn, Binder binder) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql, new String[] {idColumn})) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key for " + idColumn);
                }
                return keys.getLong(1);
            }
        }
    }

    protected int update(String sql, Binder binder) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    /**
     * Runs work in ONE transaction: commit on success, rollback on any failure (UC06 extension
     * "Database timeout: System rolls back the change"). Every SQLException becomes DataAccessException.
     */
    protected <R> R inTransaction(SqlWork<R> work) {
        try {
            connection.setAutoCommit(false);
            try {
                R result = work.run();
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Unable to save to the database - the change was rolled back", e);
        }
    }

    /** Read-only query wrapper: SQLException -> DataAccessException. */
    protected <R> R query(SqlWork<R> work) {
        try {
            return work.run();
        } catch (SQLException e) {
            throw new DataAccessException("Unable to connect to database", e);
        }
    }

    protected static Timestamp ts(LocalDateTime t) {
        return t == null ? null : Timestamp.valueOf(t);
    }

    protected static LocalDateTime ldt(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
