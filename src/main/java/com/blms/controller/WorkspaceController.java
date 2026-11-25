package com.blms.controller;

import com.blms.domain.Workspace;
import com.blms.dto.SmtpCredentials;
import com.blms.dto.WorkspaceForm;
import com.blms.dto.WorkspaceResult;
import com.blms.persistence.WorkspaceRepository;
import com.blms.security.CredentialEncryptor;

/**
 * D5 Figure 2 «Controller» WorkspaceController - UC02 Configure Company Workspace (SD02).
 * Associations: +workspace 1 Workspace, +repo 1 WorkspaceRepository, +encryptor 1 CredentialEncryptor.
 */
public class WorkspaceController {

    private final Workspace workspace;
    private final WorkspaceRepository repo;
    private final CredentialEncryptor encryptor;

    public WorkspaceController(Workspace workspace, WorkspaceRepository repo, CredentialEncryptor encryptor) {
        this.workspace = workspace;
        this.repo = repo;
        this.encryptor = encryptor;
    }

    /** SSD/SD02 steps 1-4: openWorkspaceSettings() -> settingsForm(). The SMTP password is never returned. */
    public WorkspaceForm openWorkspaceSettings() {
        // TODO(Sufiyan, UC02): ws = repo.findCurrent(); return new WorkspaceForm(ws fields, no password)
        throw new UnsupportedOperationException("TODO UC02 openWorkspaceSettings");
    }

    /**
     * SSD/SD02 steps 5-15: saveWorkspace(companyName, targetIndustry, services, smtpCredentials).
     * 6. cipher = encryptor.encrypt(smtp.password())   8. workspace.updateDetails(...)
     * 9. workspace.setSmtpCredentials(host, port, user, cipher)   10-12. icp = workspace.generateIcp()
     * 13. repo.save(workspace)   15. return new WorkspaceResult(workspaceId, icpId, true)
     * UC02 extension "missing fields" -> ValidationException from Workspace (UI highlights in red).
     */
    public WorkspaceResult saveWorkspace(String companyName, String targetIndustry, String services,
                                         SmtpCredentials smtp) {
        // TODO(Sufiyan, UC02)
        throw new UnsupportedOperationException("TODO UC02 saveWorkspace");
    }
}
