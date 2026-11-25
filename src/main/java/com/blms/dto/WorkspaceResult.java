package com.blms.dto;

/** Return type of WorkspaceController.saveWorkspace (D5 Fig 2) = SSD UC02 workspaceConfigured(workspaceId, icpId, success). */
public record WorkspaceResult(Long workspaceId, Long icpId, boolean success) {
}
