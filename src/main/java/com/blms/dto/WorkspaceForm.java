package com.blms.dto;

/** Return type of WorkspaceController.openWorkspaceSettings (D5 Fig 2) = SSD UC02 settingsForm(). Password is never sent to the UI. */
public record WorkspaceForm(String companyName, String targetIndustry, String servicesOffered,
                            String smtpHost, int smtpPort, String smtpUsername) {
}
