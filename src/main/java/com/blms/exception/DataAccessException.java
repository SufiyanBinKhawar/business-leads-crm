package com.blms.exception;

/** Wraps every SQLException thrown by a JdbcRepository (UC01/UC06 DB extensions). */
public class DataAccessException extends BlmsException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
