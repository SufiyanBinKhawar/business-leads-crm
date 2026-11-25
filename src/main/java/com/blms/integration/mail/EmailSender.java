package com.blms.integration.mail;

import com.blms.dto.SmtpCredentials;

/** D5 Figure 3 «interface» EmailSender (Protected Variations: hides Jakarta Mail). */
public interface EmailSender {

    /** SD11 step 13. SMTP auth error / invalid recipient -> IntegrationException. */
    void send(String to, String subject, String body);

    /**
     * NOT IN D5 - approved addition (CONFLICTS.md gap G3): gives the sender the decrypted SMTP settings that
     * OutreachDispatcher.dispatch(drafts, smtp) receives. Add to Figure 3 before final submission.
     */
    void configure(SmtpCredentials smtp);
}
