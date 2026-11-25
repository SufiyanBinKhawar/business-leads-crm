package com.blms.exception;

/** UC03 extension: a lead with the same email domain already exists. */
public class DuplicateLeadException extends BlmsException {

    private static final long serialVersionUID = 1L;

    public DuplicateLeadException(String message) {
        super(message);
    }

    public DuplicateLeadException(String message, Throwable cause) {
        super(message, cause);
    }
}
