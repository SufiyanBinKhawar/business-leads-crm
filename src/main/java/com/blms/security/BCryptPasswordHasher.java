package com.blms.security;

import org.mindrot.jbcrypt.BCrypt;

/** D5 Figure 2 BCryptPasswordHasher realizes PasswordHasher (UC01: passwords are never stored in plain text). */
public class BCryptPasswordHasher implements PasswordHasher {

    @Override
    public boolean matches(String raw, String hash) {
        if (raw == null || hash == null || !hash.startsWith("$2")) {
            return false;
        }
        return BCrypt.checkpw(raw, hash);
    }
}
