package com.blms.domain;

import java.time.LocalDateTime;

/** D5 Figure 1 ApiCallLog - aggregated by Workspace (records API activity, +apiLogs 0..*). */
public class ApiCallLog {

    private Long callId;
    private final String providerName;
    private final String operation;
    private final int statusCode;
    private final String errorMessage;
    private final LocalDateTime occurredAt;

    /** SD05 step 24: «create» ApiCallLog(provider, operation, statusCode, error). */
    public ApiCallLog(String providerName, String operation, int statusCode, String errorMessage) {
        this(null, providerName, operation, statusCode, errorMessage, LocalDateTime.now());
    }

    public ApiCallLog(Long callId, String providerName, String operation, int statusCode, String errorMessage,
                      LocalDateTime occurredAt) {
        this.callId = callId;
        this.providerName = providerName;
        this.operation = operation;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.occurredAt = occurredAt;
    }

    public Long getCallId() { return callId; }
    public void setCallId(Long callId) { this.callId = callId; }
    public String getProviderName() { return providerName; }
    public String getOperation() { return operation; }
    public int getStatusCode() { return statusCode; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
