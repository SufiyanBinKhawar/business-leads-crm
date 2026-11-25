package com.blms.util;

import com.blms.exception.ValidationException;

import java.util.regex.Pattern;

/** Shared input validation (approved helper; not drawn in D5). Rubric: "input validation for all values". */
public final class Validator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validator() {
    }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    public static boolean isEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static String requireEmail(String email) {
        if (!isEmail(email)) {
            throw new ValidationException("Invalid email format");
        }
        return email.trim().toLowerCase();
    }

    public static int requirePort(int port) {
        if (port < 1 || port > 65_535) {
            throw new ValidationException("SMTP port must be between 1 and 65535 (e.g. 465 or 587)");
        }
        return port;
    }

    public static String emailDomain(String email) {
        return requireEmail(email).substring(email.trim().indexOf('@') + 1);
    }
}
