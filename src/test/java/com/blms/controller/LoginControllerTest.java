package com.blms.controller;

import com.blms.domain.UserAccount;
import com.blms.domain.UserRole;
import com.blms.dto.LoginResult;
import com.blms.exception.AuthenticationException;
import com.blms.persistence.UserRepository;
import com.blms.security.BCryptPasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** SD01 with the real BCrypt hash from db/03_seed.sql (admin / admin123). */
class LoginControllerTest {

    private static final String ADMIN_HASH = "$2a$10$GOghxFrqSuiVyx/LqpmkAOAcrpvZpyLryQM3/0W2bdQZGpwuR.u7e";

    private final UserRepository users = new UserRepository(null) {
        @Override
        public UserAccount findByUsername(String username) {
            return "admin".equals(username) ? new UserAccount(1L, "admin", ADMIN_HASH, UserRole.ADMIN, true) : null;
        }
    };

    @Test
    void loginCreatesSession() {
        LoginController c = new LoginController(new BCryptPasswordHasher(), users);
        LoginResult r = c.login("admin", "admin123");
        assertEquals(UserRole.ADMIN, r.role());
        assertNotNull(c.getSession());
        assertEquals("admin", c.getSession().getUser().getUsername());
    }

    @Test
    void wrongPasswordOrUnknownUserIsRejected() {
        LoginController c = new LoginController(new BCryptPasswordHasher(), users);
        assertThrows(AuthenticationException.class, () -> c.login("admin", "wrong"));
        assertThrows(AuthenticationException.class, () -> c.login("ghost", "admin123"));
        assertNull(c.getSession());
    }
}
