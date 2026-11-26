package com.blms.integration;

import com.blms.dto.SmtpCredentials;
import com.blms.exception.IntegrationException;
import com.blms.integration.llm.OpenAiLlmClient;
import com.blms.integration.mail.JakartaMailSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gap G12 offline modes and the checks the adapters make before any network call. */
class AdapterOfflineAndValidationTest {

    @Test
    void llmOfflineModeReturnsSubjectBlankLineBody() {
        String text = new OpenAiLlmClient("").complete("any prompt");
        int split = text.indexOf("\n\n");
        assertTrue(split > 0);
        assertTrue(text.substring(split + 2).length() > 20);
    }

    @Test
    void mailOfflineModeLogsInsteadOfSending() {
        JakartaMailSender sender = new JakartaMailSender();
        sender.configure(new SmtpCredentials(null, 587, null, null));
        assertDoesNotThrow(() -> sender.send("sara@acme.example", "Hi", "Body"));
    }

    @Test
    void uc11InvalidRecipientIsRejectedBeforeConnecting() {
        JakartaMailSender sender = new JakartaMailSender();
        sender.configure(new SmtpCredentials("smtp.invalid", 587, "sales@demo.example", "pw"));
        IntegrationException e = assertThrows(IntegrationException.class,
                () -> sender.send("not an address", "Hi", "Body"));
        assertTrue(e.getMessage().startsWith("Invalid recipient"));
    }
}
