package com.blms.domain;

import com.blms.TestData;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** CONFLICTS.md S-004: stored drafts and send attempts can be attached when a lead is loaded. */
class LoadHelpersTest {

    @Test
    void loadedDraftAndAttemptAreNavigable() {
        Lead lead = TestData.lead();
        Contact c = lead.getPrimaryContact();
        EmailDraft d = new EmailDraft(5L, "Hi", "Body", DraftStatus.APPROVED, LocalDateTime.now(), LocalDateTime.now(), c);
        lead.addLoadedDraft(d);
        OutreachDispatch failed = new OutreachDispatch(9L, LocalDateTime.now(), "FAILED", "Invalid recipient: x", c);
        d.addLoadedAttempt(failed);

        assertSame(d, lead.getDrafts().get(0));
        assertEquals("FAILED", lead.getDrafts().get(0).getAttempts().get(0).getDeliveryStatus());
        assertSame(failed, c.getDispatches().get(0));            // Contact +dispatches stays consistent
        assertEquals(DraftStatus.APPROVED, d.getStatus());       // loading never changes the status
    }
}
