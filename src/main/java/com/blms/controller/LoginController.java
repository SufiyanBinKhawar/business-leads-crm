package com.blms.controller;

import com.blms.domain.Session;
import com.blms.domain.UserAccount;
import com.blms.dto.LoginResult;
import com.blms.exception.AuthenticationException;
import com.blms.persistence.UserRepository;
import com.blms.security.PasswordHasher;
import com.blms.util.Validator;

/**
 * D5 Figure 2 «Controller» LoginController - UC01 Authenticate System User (SD01).
 * Associations: +hasher 1 PasswordHasher, +users 1 UserRepository, creates +session 0..1 Session.
 * REFERENCE IMPLEMENTATION: every other controller follows this shape.
 */
public class LoginController {

    private Session session;
    private final PasswordHasher hasher;
    private final UserRepository users;

    public LoginController(PasswordHasher hasher, UserRepository users) {
        this.hasher = hasher;
        this.users = users;
    }

    /** SSD/SD01: login(username, password) -> loginSuccessful(userId, role, dashboard). */
    public LoginResult login(String username, String password) {
        Validator.requireText(username, "Username");
        Validator.requireText(password, "Password");
        UserAccount user = users.findByUsername(username.trim());                   // SD01 steps 2-3
        if (user == null || !user.verifyPassword(password, hasher)) {               // SD01 steps 4-7
            throw new AuthenticationException("Incorrect username or password");    // UC01 extension
        }
        session = new Session(user);                                                // SD01 step 8 (Creator)
        return new LoginResult(user.getUserId(), user.getUsername(), user.getRole()); // SD01 step 9
    }

    /** Accessor of the D5 attribute "- session : Session" (null before login). */
    public Session getSession() {
        return session;
    }
}
