package com.blms.dto;

/**
 * D5 type used by saveWorkspace(..., smtp), Workspace.getSmtpCredentials(), OutreachDispatcher.dispatch(..., smtp)
 * and JakartaMailSender.-smtp. Coming FROM the UI the password is plain text; coming FROM Workspace it is the
 * encrypted cipher text (see CONFLICTS.md gap G2).
 */
public record SmtpCredentials(String host, int port, String username, String password) {

    public SmtpCredentials withPassword(String newPassword) {
        return new SmtpCredentials(host, port, username, newPassword);
    }

    @Override
    public String toString() {
        return "SmtpCredentials[host=" + host + ", port=" + port + ", username=" + username + ", password=***]";
    }
}
