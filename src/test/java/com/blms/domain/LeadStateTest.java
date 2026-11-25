package com.blms.domain;

import com.blms.TestData;
import com.blms.domain.state.DiscoveredState;
import com.blms.exception.InvalidStateTransitionException;
import com.blms.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SD03 Creator chain and SD08 State pattern. */
class LeadStateTest {

    @Test
    void workspaceCreatesLeadWithContactAndDiscoveredState() {
        Lead lead = TestData.lead();
        assertEquals(PipelineStatus.DISCOVERED, lead.getStatus());
        assertEquals("sara@acme.example", lead.getPrimaryContact().getEmail());
        assertEquals("rep", lead.getCreator().getUsername());
    }

    @Test
    void discoveredCannotJumpToClosedWon() {
        Lead lead = TestData.lead();
        assertFalse(new DiscoveredState().canMoveTo(PipelineStatus.CLOSED_WON));
        assertThrows(InvalidStateTransitionException.class, () -> lead.changeStatus(PipelineStatus.CLOSED_WON));
        assertEquals(PipelineStatus.DISCOVERED, lead.getStatus());
    }

    @Test
    void happyPathThroughThePipeline() {
        Lead lead = TestData.lead();
        lead.changeStatus(PipelineStatus.QUALIFIED);
        lead.changeStatus(PipelineStatus.CONTACTED);
        lead.changeStatus(PipelineStatus.CLOSED_WON);
        assertEquals(PipelineStatus.CLOSED_WON, lead.getStatus());
        assertThrows(InvalidStateTransitionException.class, () -> lead.changeStatus(PipelineStatus.CLOSED_LOST));
    }

    @Test
    void archiveIsSoftDeleteAndFilterHidesIt() {
        Lead lead = TestData.lead();
        lead.archive();
        assertTrue(lead.isArchived());
        assertFalse(lead.matches(com.blms.dto.LeadFilter.all()));
    }

    @Test
    void invalidEmailRejected() {
        assertThrows(ValidationException.class, () -> TestData.workspace().createLead(TestData.data("Acme", "nope")));
    }

    @Test
    void metricsCountOnlyActiveLeads() {
        Lead a = TestData.lead();
        Lead b = TestData.lead();
        b.changeStatus(PipelineStatus.QUALIFIED);
        Lead c = TestData.lead();
        c.archive();
        PipelineMetrics m = new PipelineMetrics();
        m.recalculate(java.util.List.of(a, b, c));
        assertEquals(2, m.getTotalLeads());
        assertEquals(1, m.getDiscoveredCount());
        assertEquals(1, m.getQualifiedCount());
    }
}
