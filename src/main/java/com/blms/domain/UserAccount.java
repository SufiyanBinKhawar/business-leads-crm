package com.blms.domain;

import com.blms.security.PasswordHasher;

/** D5 Figure 1 «Information Expert» UserAccount - owns passwordHash, so it verifies its own password (SD01 step 4). */
public class UserAccount {

    private Long userId;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final boolean isActive;

    public UserAccount(Long userId, String username, String passwordHash, UserRole role, boolean isActive) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
    }

    /** SD01 steps 4-7. */
    public boolean verifyPassword(String raw, PasswordHasher hasher) {
        return isActive && raw != null && hasher.matches(raw, passwordHash);
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return isActive; }
}
