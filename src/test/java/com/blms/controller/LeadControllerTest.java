package com.blms.controller;

import com.blms.TestData;
import com.blms.domain.InteractionLog;
import com.blms.domain.Lead;
import com.blms.domain.PipelineMetrics;
import com.blms.domain.PipelineStatus;
import com.blms.domain.Session;
import com.blms.dto.LeadData;
import com.blms.dto.LeadFilter;
import com.blms.exception.AuthenticationException;
import com.blms.exception.DataAccessException;
import com.blms.exception.DuplicateLeadException;
import com.blms.exception.InvalidStateTransitionException;
import com.blms.exception.ValidationException;
import com.blms.persistence.LeadRepository;
import com.blms.util.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC03, UC06, UC07, UC08 (SD03, SD06, SD07, SD08) with an in-memory LeadRepository - no database. */
class LeadControllerTest {

    /** Repository double: keeps leads in a map; the Connection is never used. */
    static class InMemoryLeadRepository extends LeadRepository {
        final Map<Long, Lead> rows = new LinkedHashMap<>();
        final List<String> calls = new ArrayList<>();
        long nextId = 1;
        boolean failNextSave;

        InMemoryLeadRepository() {
            super(null);
        }

        @Override
        public boolean existsByEmailDomain(String email) {
            calls.add("existsByEmailDomain");
            String domain = Validator.emailDomain(email);
            return rows.values().stream().anyMatch(l -> l.getContacts().stream()
                    .anyMatch(c -> c.getEmail().endsWith("@" + domain)));
        }

        @Override
        public Lead save(Lead lead) {
            calls.add("save");
            if (failNextSave) {
                failNextSave = false;
                throw new DataAccessException("Unable to save to the database - the change was rolled back");
            }
            if (lead.getLeadId() == null) {
                lead.setLeadId(nextId++);
            }
            rows.put(lead.getLeadId(), lead);
            return lead;
        }

        @Override
        public Lead findById(Long id) {
            calls.add("findById");
            return rows.get(id);
        }

        @Override
        public List<Lead> findByFilter(LeadFilter f) {
            calls.add("findByFilter");
            return rows.values().stream().filter(l -> l.matches(f)).toList();
        }

        @Override
        public List<Lead> findAll() {
            calls.add("findAll");
            return new ArrayList<>(rows.values());
        }
    }

    private InMemoryLeadRepository repo;
    private Session session;
    private LeadController controller;

    @BeforeEach
    void setUp() {
        repo = new InMemoryLeadRepository();
        session = new Session(TestData.rep());
        controller = new LeadController(() -> session, repo, new PipelineMetrics(), TestData.workspace());
    }

    // ---------------------------------------------------------------- UC03

    @Test
    void saveLeadCreatesDiscoveredLeadInSd03Order() {
        Lead lead = controller.saveLead("Acme", "Sara Malik", "sara@acme.example", "CTO");
        assertNotNull(lead.getLeadId());
        assertEquals(PipelineStatus.DISCOVERED, lead.getStatus());
        assertEquals("rep", lead.getCreator().getUsername());
        assertEquals(List.of("existsByEmailDomain", "save"), repo.calls);
    }

    @Test
    void uc03DuplicateEmailDomainIsRejected() {
        controller.saveLead("Acme", "Sara Malik", "sara@acme.example", "CTO");
        DuplicateLeadException e = assertThrows(DuplicateLeadException.class,
                () -> controller.saveLead("Acme 2", "Omar", "omar@acme.example", "CFO"));
        assertTrue(e.getMessage().contains("already exists"));
        assertEquals(1, repo.rows.size());
    }

    @Test
    void uc03InvalidEmailIsRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> controller.saveLead("Acme", "Sara", "not-an-email", "CTO"));
        assertEquals("Invalid email format", e.getMessage());
        assertTrue(repo.rows.isEmpty());
    }

    @Test
    void saveLeadNeedsALoggedInUser() {
        session = null;
        assertThrows(AuthenticationException.class,
                () -> controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO"));
    }

    // ---------------------------------------------------------------- UC06

    @Test
    void filterLeadsRemembersTheFilterForTheReload() {
        controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO");
        Lead other = controller.saveLead("Beta", "Ali", "ali@beta.example", "CEO");
        LeadFilter byText = new LeadFilter(null, null, "beta", false);
        assertEquals(List.of(other), controller.filterLeads(byText));
        List<Lead> after = controller.saveLeadChanges(other.getLeadId(),
                new LeadData("Beta Ltd", "Ali", "ali@beta.example", "CEO", "Tech", "123", null), false);
        assertEquals(1, after.size());
        assertEquals("Beta Ltd", after.get(0).getCompanyName());
    }

    @Test
    void saveLeadChangesArchivesInsteadOfDeleting() {
        Lead lead = controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO");
        repo.calls.clear();
        List<Lead> visible = controller.saveLeadChanges(lead.getLeadId(),
                new LeadData("Acme", "Sara", "sara@acme.example", "CTO", null, "555", null), true);
        assertTrue(repo.rows.get(lead.getLeadId()).isArchived());
        assertTrue(visible.isEmpty());
        assertEquals(List.of("findById", "save", "findByFilter"), repo.calls);   // SD06 steps 11-17
    }

    @Test
    void uc06DatabaseTimeoutReportsTheRollback() {
        Lead lead = controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO");
        repo.failNextSave = true;
        DataAccessException e = assertThrows(DataAccessException.class, () -> controller.saveLeadChanges(
                lead.getLeadId(), new LeadData("Acme", "Sara", "sara@acme.example", "CTO", null, null, null), true));
        assertTrue(e.getMessage().contains("rolled back"));
    }

    @Test
    void editLeadOfUnknownIdIsReported() {
        assertThrows(ValidationException.class, () -> controller.editLead(99L));
    }

    // ---------------------------------------------------------------- UC07

    @Test
    void submitInteractionStampsAuthorAndTime() {
        Lead lead = controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO");
        repo.calls.clear();
        InteractionLog log = controller.submitInteraction(lead.getLeadId(), "Called, interested");
        assertEquals("rep", log.getAuthor().getUsername());
        assertNotNull(log.getCreatedAt());
        assertEquals(List.of("findById", "save"), repo.calls);                    // SD07 steps 6-11
        assertEquals(1, repo.rows.get(lead.getLeadId()).getHistory().size());
    }

    @Test
    void uc07EmptyNoteIsRejected() {
        Lead lead = controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO");
        assertThrows(ValidationException.class, () -> controller.submitInteraction(lead.getLeadId(), "   "));
        assertTrue(repo.rows.get(lead.getLeadId()).getHistory().isEmpty());
    }

    // ---------------------------------------------------------------- UC08

    @Test
    void changePipelineStatusMovesAndRecalculates() {
        Lead lead = controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO");
        assertEquals(PipelineStatus.DISCOVERED, controller.selectLead(lead.getLeadId()));
        PipelineMetrics m = controller.changePipelineStatus(lead.getLeadId(), PipelineStatus.QUALIFIED);
        assertEquals(1, m.getQualifiedCount());
        assertEquals(0, m.getDiscoveredCount());
        assertEquals(PipelineStatus.QUALIFIED, repo.rows.get(lead.getLeadId()).getStatus());
    }

    @Test
    void uc08InvalidTransitionIsRejectedAndNothingIsSaved() {
        Lead lead = controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO");
        repo.calls.clear();
        assertThrows(InvalidStateTransitionException.class,
                () -> controller.changePipelineStatus(lead.getLeadId(), PipelineStatus.CLOSED_WON));
        assertFalse(repo.calls.contains("save"));
        assertEquals(PipelineStatus.DISCOVERED, controller.selectLead(lead.getLeadId()));
    }

    @Test
    void getPipelineMetricsCountsBeforeAnyChange() {
        controller.saveLead("Acme", "Sara", "sara@acme.example", "CTO");
        controller.saveLead("Beta", "Ali", "ali@beta.example", "CEO");
        PipelineMetrics m = controller.getPipelineMetrics();
        assertEquals(2, m.getTotalLeads());
        assertEquals(2, m.getDiscoveredCount());
    }
}
