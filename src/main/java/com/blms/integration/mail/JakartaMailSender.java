package com.blms.integration.mail;

import com.blms.dto.SmtpCredentials;

/**
 * D5 Figure 3 JakartaMailSender realizes EmailSender (UC11) - SMTP over TLS (port 587 STARTTLS / 465 SSL).
 * Offline demo mode: if no SMTP host is configured, messages are logged instead of sent (CONFLICTS.md gap G12).
 */
public class JakartaMailSender implements EmailSender {

    private static final System.Logger LOG = System.getLogger(JakartaMailSender.class.getName());
    private SmtpCredentials smtp;

    @Override
    public void configure(SmtpCredentials smtp) {
        this.smtp = smtp;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (smtp == null || smtp.host() == null || smtp.host().isBlank()) {
            LOG.log(System.Logger.Level.INFO, "[demo mode] email to {0}: {1}", to, subject);
            return;
        }
        // TODO(Sufiyan, UC11): jakarta.mail.Session with mail.smtp.auth=true, starttls (587) or ssl (465),
        //  Transport.send(MimeMessage). AuthenticationFailedException -> IntegrationException(
        //  "SMTP authentication failed - check the email password in Workspace Settings");
        //  AddressException / SendFailedException -> IntegrationException("Invalid recipient: " + to)
        throw new UnsupportedOperationException("TODO UC11 JakartaMailSender.send");
    }
}
