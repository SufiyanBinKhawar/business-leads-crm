package com.blms.exception;

/** UC08 extension: PipelineState.canMoveTo(next) returned false. */
public class InvalidStateTransitionException extends BlmsException {

    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
