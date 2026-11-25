package com.blms.domain;

import com.blms.TestData;
import com.blms.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** SD09 (Lead creates EmailDraft), SD10 (revise/approve), SD11 (recordDispatch). */
class EmailDraftTest {

    @Test
    void reviseApproveAndDispatch() {
        EmailDraft d = TestData.lead().createDraft("Hello", "Body");
        assertEquals(DraftStatus.DRAFT, d.getStatus());
        assertEquals("sara@acme.example", d.getRecipientEmail());
        d.revise("Hi Sara", "Personal touch");
        d.approve();
        assertEquals(DraftStatus.APPROVED, d.getStatus());
        assertNotNull(d.getApprovedAt());
        OutreachDispatch od = d.recordDispatch("SENT");
        assertEquals(DraftStatus.SENT, d.getStatus());
        assertEquals(1, d.getRecipient().getDispatches().size());
        assertEquals("SENT", od.getDeliveryStatus());
    }

    @Test
    void emptyDraftCannotBeApproved() {
        EmailDraft d = TestData.lead().createDraft("Hello", "Body");
        assertThrows(ValidationException.class, () -> d.revise("Hello", "  "));
    }
}
