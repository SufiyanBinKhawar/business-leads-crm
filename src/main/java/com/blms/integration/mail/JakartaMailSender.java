package com.blms.integration.mail;

import com.blms.dto.SmtpCredentials;
import com.blms.exception.IntegrationException;
import jakarta.mail.Authenticator;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.SendFailedException;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * D5 Figure 3 JakartaMailSender realizes EmailSender (UC11) - SMTP over TLS (port 587 STARTTLS / 465 SSL).
 * GoF Adapter: hides Jakarta Mail behind EmailSender (Protected Variations).
 * Offline demo mode: if no SMTP host is configured, messages are logged instead of sent (CONFLICTS.md gap G12).
 */
public class JakartaMailSender implements EmailSender {

    private static final System.Logger LOG = System.getLogger(JakartaMailSender.class.getName());
    private static final String TIMEOUT_MS = "15000";
    private SmtpCredentials smtp;

    @Override
    public void configure(SmtpCredentials smtp) {
        this.smtp = smtp;
    }

    /**
     * SD11 step 13. UC11 extensions: wrong password -> IntegrationException("SMTP authentication failed - ...")
     * (aborts the batch); bad address -> IntegrationException("Invalid recipient: ...") (that draft only).
     */
    @Override
    public void send(String to, String subject, String body) {
        if (smtp == null || smtp.host() == null || smtp.host().isBlank()) {
            LOG.log(System.Logger.Level.INFO, "[demo mode] email to {0}: {1}", to, subject);
            return;
        }
        InternetAddress recipient;
        try {
            recipient = new InternetAddress(to, true);
        } catch (AddressException e) {
            throw new IntegrationException("Invalid recipient: " + to, e);
        }
        try {
            MimeMessage message = new MimeMessage(jakarta.mail.Session.getInstance(properties(), new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtp.username(), smtp.password());
                }
            }));
            message.setFrom(new InternetAddress(smtp.username()));
            message.setRecipient(Message.RecipientType.TO, recipient);
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setText(body, StandardCharsets.UTF_8.name());      // D3 UC10: newlines/paragraphs kept
            Transport.send(message);
        } catch (AuthenticationFailedException e) {
            throw new IntegrationException(
                    "SMTP authentication failed - check the email password in Workspace Settings", e);
        } catch (SendFailedException e) {
            throw new IntegrationException("Invalid recipient: " + to, e);
        } catch (MessagingException e) {
            throw new IntegrationException("SMTP server unreachable - check the host and port in Workspace Settings", e);
        }
    }

    /** 465 = implicit SSL, anything else = STARTTLS required (D3 UC11 "Jakarta Mail ... via TLS"). */
    private Properties properties() {
        Properties p = new Properties();
        p.put("mail.smtp.host", smtp.host());
        p.put("mail.smtp.port", String.valueOf(smtp.port()));
        p.put("mail.smtp.auth", "true");
        if (smtp.port() == 465) {
            p.put("mail.smtp.ssl.enable", "true");
        } else {
            p.put("mail.smtp.starttls.enable", "true");
            p.put("mail.smtp.starttls.required", "true");
        }
        p.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        p.put("mail.smtp.timeout", TIMEOUT_MS);
        p.put("mail.smtp.writetimeout", TIMEOUT_MS);
        return p;
    }
}
