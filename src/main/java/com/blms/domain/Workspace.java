package com.blms.domain;

import com.blms.dto.LeadData;
import com.blms.dto.SmtpCredentials;
import com.blms.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D5 Figure 1 «Information Expert» Workspace.
 * Composes IdealCustomerProfile (+icp 0..1); aggregates UserAccount (+users), Lead (+leads), ApiCallLog (+apiLogs).
 * Creator of IdealCustomerProfile (SD02), Lead (SD03/04/05) and ApiCallLog (SD05).
 */
public class Workspace {

    private Long workspaceId;
    private String companyName;
    private String targetIndustry;
    private String servicesOffered;
    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String smtpCredentialEncrypted;
    private IdealCustomerProfile icp;
    private final List<UserAccount> users = new ArrayList<>();
    private final List<Lead> leads = new ArrayList<>();
    private final List<ApiCallLog> apiLogs = new ArrayList<>();

    public Workspace(Long workspaceId, String companyName, String targetIndustry, String servicesOffered,
                     String smtpHost, int smtpPort, String smtpUsername, String smtpCredentialEncrypted) {
        this.workspaceId = workspaceId;
        this.companyName = companyName;
        this.targetIndustry = targetIndustry;
        this.servicesOffered = servicesOffered;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpCredentialEncrypted = smtpCredentialEncrypted;
    }

    /** SD02 step 8. UC02 extension: missing required fields. */
    public void updateDetails(String name, String industry, String services) {
        this.companyName = Validator.requireText(name, "Company name");
        this.targetIndustry = Validator.requireText(industry, "Target industry");
        this.servicesOffered = Validator.requireText(services, "Services offered");
    }

    /** SD02 steps 10-12: Information Expert + Creator of its ICP. */
    public IdealCustomerProfile generateIcp() {
        this.icp = new IdealCustomerProfile(targetIndustry, servicesOffered);
        return icp;
    }

    /** SD03 steps 6-10, SD04 step 13, SD05 step 18: Workspace aggregates leads, so it creates them. */
    public Lead createLead(LeadData data) {
        Lead lead = new Lead(data);
        leads.add(lead);
        return lead;
    }

    /** SD02 step 9: stores the ALREADY ENCRYPTED password (cipher). Plain text never reaches this class. */
    public void setSmtpCredentials(String host, int port, String user, String cipher) {
        this.smtpHost = Validator.requireText(host, "SMTP host");
        this.smtpPort = Validator.requirePort(port);
        this.smtpUsername = Validator.requireText(user, "SMTP username");
        this.smtpCredentialEncrypted = Validator.requireText(cipher, "SMTP password");
    }

    /** SD05 steps 2-3. */
    public IdealCustomerProfile getIcp() {
        return icp;
    }

    /** SD11 steps 8-9. The password field holds the ENCRYPTED value (see CONFLICTS.md gap G2). */
    public SmtpCredentials getSmtpCredentials() {
        return new SmtpCredentials(smtpHost, smtpPort, smtpUsername, smtpCredentialEncrypted);
    }

    /** SD05 steps 23-25: Workspace aggregates ApiCallLog, so it creates the entry. */
    public ApiCallLog logApiCall(String provider, String operation, int statusCode, String error) {
        ApiCallLog log = new ApiCallLog(provider, operation, statusCode, error);
        apiLogs.add(log);
        return log;
    }

    /** Used by WorkspaceRepository when loading. */
    public void setLoadedIcp(IdealCustomerProfile icp) { this.icp = icp; }

    public Long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }
    public String getCompanyName() { return companyName; }
    public String getTargetIndustry() { return targetIndustry; }
    public String getServicesOffered() { return servicesOffered; }
    public String getSmtpHost() { return smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public String getSmtpUsername() { return smtpUsername; }
    public String getSmtpCredentialEncrypted() { return smtpCredentialEncrypted; }
    public List<UserAccount> getUsers() { return Collections.unmodifiableList(users); }
    public List<Lead> getLeads() { return Collections.unmodifiableList(leads); }
    public List<ApiCallLog> getApiLogs() { return Collections.unmodifiableList(apiLogs); }
}
