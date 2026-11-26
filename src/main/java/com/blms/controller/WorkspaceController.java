package com.blms.controller;

import com.blms.domain.IdealCustomerProfile;
import com.blms.domain.Workspace;
import com.blms.dto.SmtpCredentials;
import com.blms.dto.WorkspaceForm;
import com.blms.dto.WorkspaceResult;
import com.blms.exception.ValidationException;
import com.blms.persistence.WorkspaceRepository;
import com.blms.security.CredentialEncryptor;
import com.blms.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
        Workspace ws = repo.findCurrent();                                     // SD02 steps 2-3
        if (ws == null) {
            throw new ValidationException("No workspace found - run db/03_seed.sql");
        }
        return new WorkspaceForm(ws.getCompanyName(), ws.getTargetIndustry(), ws.getServicesOffered(),
                ws.getSmtpHost(), ws.getSmtpPort(), ws.getSmtpUsername());        // SD02 step 4 (no password)
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
        SmtpCredentials creds = smtp == null ? new SmtpCredentials(null, 0, null, null) : smtp;
        requireAllFields(companyName, targetIndustry, services, creds);   // UC02 extensions, before anything changes
        String cipher = encryptor.encrypt(creds.password());                                // SD02 steps 6-7
        workspace.updateDetails(companyName, targetIndustry, services);                     // SD02 step 8
        workspace.setSmtpCredentials(creds.host().trim(), creds.port(), creds.username().trim(), cipher); // step 9
        IdealCustomerProfile icp = workspace.generateIcp();                                 // SD02 steps 10-12
        repo.save(workspace);                                                               // SD02 steps 13-14
        return new WorkspaceResult(workspace.getWorkspaceId(), icp.getIcpId(), true);      // SD02 step 15
    }

    // ------------------------------------------------------------ private helpers

    private static final Pattern HOST = Pattern.compile("^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$");

    /**
     * D3 UC02 "Missing required fields: System highlights empty fields in red and prevents saving": every empty
     * field is named in ONE message and nothing is changed. Gap G13: SMTP host and port are format-checked here; a
     * wrong password surfaces in UC11 ("SMTP Authentication Error").
     */
    private static void requireAllFields(String companyName, String targetIndustry, String services,
                                         SmtpCredentials smtp) {
        List<String> missing = new ArrayList<>();
        addIfBlank(missing, companyName, "Company name");
        addIfBlank(missing, targetIndustry, "Target industry");
        addIfBlank(missing, services, "Services offered");
        addIfBlank(missing, smtp.host(), "SMTP host");
        if (smtp.port() == 0) {
            missing.add("SMTP port");
        }
        addIfBlank(missing, smtp.username(), "SMTP username");
        addIfBlank(missing, smtp.password(), "SMTP password");
        if (!missing.isEmpty()) {
            throw new ValidationException("Please fill in the required fields: " + String.join(", ", missing));
        }
        Validator.requirePort(smtp.port());
        if (!HOST.matcher(smtp.host().trim()).matches()) {
            throw new ValidationException("Invalid SMTP host - check the server name (e.g. smtp.gmail.com) and port");
        }
    }

    private static void addIfBlank(List<String> missing, String value, String label) {
        if (value == null || value.isBlank()) {
            missing.add(label);
        }
    }
}
