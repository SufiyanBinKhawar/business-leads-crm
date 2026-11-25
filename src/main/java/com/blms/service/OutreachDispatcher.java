package com.blms.service;

import com.blms.domain.EmailDraft;
import com.blms.domain.OutreachDispatch;
import com.blms.dto.SmtpCredentials;
import com.blms.exception.IntegrationException;
import com.blms.integration.mail.EmailSender;

import java.util.List;

/** D5 Figure 3 «Pure Fabrication» OutreachDispatcher (+sender EmailSender). */
public class OutreachDispatcher {

    private final EmailSender sender;

    public OutreachDispatcher(EmailSender sender) {
        this.sender = sender;
    }

    /**
     * SD11 steps 10-17. Returns sentCount.
     * UC11 extension "invalid recipient": recorded as a FAILED attempt with the error, draft stays APPROVED.
     * UC11 extension "SMTP authentication error": IntegrationException propagates and aborts the batch.
     * @param smtp DECRYPTED credentials (decrypted by OutreachController - CONFLICTS.md gap G2)
     */
    public int dispatch(List<EmailDraft> drafts, SmtpCredentials smtp) {
        sender.configure(smtp);
        int sent = 0;
        for (EmailDraft d : drafts) {
            String to = d.getRecipientEmail();
            try {
                sender.send(to, d.getSubject(), d.getBody());
                d.recordDispatch("SENT");
                sent++;
            } catch (IntegrationException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("SMTP authentication")) {
                    throw e;
                }
                OutreachDispatch failed = d.recordDispatch("FAILED");
                failed.setErrorMessage(e.getMessage());
            }
        }
        return sent;
    }
}
