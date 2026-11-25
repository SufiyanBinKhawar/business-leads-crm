package com.blms.exception;

/** Invalid user input (D3 extensions: missing fields, invalid email, empty note/draft, bad port). */
public class ValidationException extends BlmsException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
