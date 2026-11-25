package com.blms.domain;

/** D5 Figure 1 «enumeration» PipelineStatus. Stored in LEADS.STATUS as name(). */
public enum PipelineStatus {
    DISCOVERED,
    QUALIFIED,
    CONTACTED,
    CLOSED_WON,
    CLOSED_LOST
}
