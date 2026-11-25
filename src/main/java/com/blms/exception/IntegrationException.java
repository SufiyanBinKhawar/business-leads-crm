package com.blms.exception;

/** Failure of an external service: B2B API (HTTP 429, timeout), LLM API, SMTP server. */
public class IntegrationException extends BlmsException {

    private static final long serialVersionUID = 1L;

    public IntegrationException(String message) {
        super(message);
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
