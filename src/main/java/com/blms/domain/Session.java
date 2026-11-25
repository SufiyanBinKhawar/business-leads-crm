package com.blms.domain;

import java.time.LocalDateTime;

/** D5 Figure 1 Session - created by LoginController (SD01 step 8); Session "for" +user 1 UserAccount. */
public class Session {

    private final Long userId;
    private final UserRole role;
    private final LocalDateTime startedAt;
    private final UserAccount user;

    public Session(UserAccount user) {
        this.user = user;
        this.userId = user.getUserId();
        this.role = user.getRole();
        this.startedAt = LocalDateTime.now();
    }

    /** SD07 step 4: Information Expert for "who is the current user". */
    public UserAccount getUser() {
        return user;
    }

    public Long getUserId() { return userId; }
    public UserRole getRole() { return role; }
    public LocalDateTime getStartedAt() { return startedAt; }
}
