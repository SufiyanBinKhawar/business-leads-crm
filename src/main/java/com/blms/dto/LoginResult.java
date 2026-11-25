package com.blms.dto;

import com.blms.domain.UserRole;

/** Return type of LoginController.login (D5 Fig 2) = SSD UC01 loginSuccessful(userId, role, dashboard). */
public record LoginResult(Long userId, String username, UserRole role) {
}
