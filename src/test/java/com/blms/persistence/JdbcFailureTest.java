package com.blms.persistence;

import com.blms.TestData;
import com.blms.controller.LoginController;
import com.blms.domain.Lead;
import com.blms.exception.DataAccessException;
import com.blms.security.BCryptPasswordHasher;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database failure extensions without a database: a JDBC Connection built with java.lang.reflect.Proxy.
 * UC01 "Database connection timeout: Unable to connect to database"; UC06 "Database timeout: System rolls back
 * the change and informs the user that the save failed".
 */
class JdbcFailureTest {

    /** Records the JDBC calls; every statement fails with a timeout after {@code okUpdates} successful updates. */
    static class FlakyDatabase {
        final List<String> calls = new ArrayList<>();
        int okUpdates;

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                    (proxy, method, args) -> {
                        calls.add(method.getName());
                        return switch (method.getName()) {
                            case "prepareStatement" -> statement();
                            case "setAutoCommit", "commit", "rollback", "close" -> null;
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    });
        }

        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] {PreparedStatement.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "executeUpdate":
                                calls.add("executeUpdate");
                                if (okUpdates-- > 0) {
                                    return 1;
                                }
                                throw new SQLTimeoutException("ORA-12170: TNS:Connect timeout occurred");
                            case "getGeneratedKeys":
                                return keys();
                            case "executeQuery":
                                throw new SQLTimeoutException("ORA-12170: TNS:Connect timeout occurred");
                            case "close":
                                return null;
                            default:
                                if (method.getName().startsWith("set")) {
                                    return null;
                                }
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }

        private ResultSet keys() {
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> true;
                        case "getLong" -> 42L;
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    @Test
    void uc01DatabaseUnavailableIsReportedAndNoSessionStarts() {
        FlakyDatabase db = new FlakyDatabase();
        LoginController login = new LoginController(new BCryptPasswordHasher(), new UserRepository(db.connection()));
        DataAccessException e = assertThrows(DataAccessException.class, () -> login.login("admin", "admin123"));
        assertEquals("Unable to connect to database", e.getMessage());
        assertNull(login.getSession());
    }

    @Test
    void uc06DatabaseTimeoutRollsBackTheWholeSave() {
        FlakyDatabase db = new FlakyDatabase();
        db.okUpdates = 1;                         // the lead row is written, the contact row times out
        LeadRepository repo = new LeadRepository(db.connection());
        Lead lead = TestData.lead();

        DataAccessException e = assertThrows(DataAccessException.class, () -> repo.save(lead));
        assertTrue(e.getMessage().contains("rolled back"));
        assertTrue(db.calls.contains("rollback"));
        assertTrue(!db.calls.contains("commit"));
        assertEquals("setAutoCommit", db.calls.get(db.calls.size() - 1));   // auto-commit restored
        assertNull(lead.getLeadId());              // the id handed out inside the failed transaction is cleared
        assertNull(lead.getPrimaryContact().getContactId());
    }

    @Test
    void readFailuresBecomeDataAccessException() {
        LeadRepository repo = new LeadRepository(new FlakyDatabase().connection());
        assertThrows(DataAccessException.class, () -> repo.findById(1L));
        assertThrows(DataAccessException.class, repo::findAll);
    }

    @Test
    void sqlExceptionIsNeverLeakedRaw() {
        DraftRepository drafts = new DraftRepository(new FlakyDatabase().connection());
        RuntimeException e = assertThrows(RuntimeException.class, () -> drafts.findById(5L));
        assertTrue(e instanceof DataAccessException);
        assertTrue(e.getCause() instanceof SQLException);
    }
}
