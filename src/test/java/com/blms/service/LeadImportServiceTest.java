package com.blms.service;

import com.blms.TestData;
import com.blms.domain.Lead;
import com.blms.domain.Session;
import com.blms.dto.ImportSummary;
import com.blms.dto.LeadData;
import com.blms.integration.source.LeadSource;
import com.blms.persistence.LeadRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** SD04/SD05: one algorithm, any LeadSource (Polymorphism). Uses a fake repository - no database. */
class LeadImportServiceTest {

    /** Repository double: overrides the two calls SD04 makes; the Connection is never used. */
    static class FakeLeadRepository extends LeadRepository {
        int saved;

        FakeLeadRepository() {
            super(null);
        }

        @Override
        public boolean existsByEmailDomain(String email) {
            return email.endsWith("@existing.example");
        }

        @Override
        public int saveAll(List<Lead> leads) {
            long id = 100;
            for (Lead l : leads) {
                l.setLeadId(id++);
            }
            saved += leads.size();
            return leads.size();
        }
    }

    @Test
    void countsImportedDuplicatesAndInvalid() {
        FakeLeadRepository repo = new FakeLeadRepository();
        LeadImportService service = new LeadImportService(repo, () -> new Session(TestData.rep()));
        LeadSource source = () -> List.of(
                new LeadData("Acme", "Sara", "sara@acme.example", "CTO", "Tech", null, null),
                new LeadData("Acme", "Omar", "omar@acme.example", "CFO", "Tech", null, null),     // same domain in file
                new LeadData("Old Co", "Ali", "ali@existing.example", "CEO", "Tech", null, null),  // already in DB
                new LeadData("Broken", "", "not-an-email", null, null, null, null));               // irregular row
        ImportSummary s = service.importFrom(source, TestData.workspace());
        assertEquals(1, s.imported());
        assertEquals(2, s.duplicates());
        assertEquals(1, s.invalid());
        assertEquals(List.of(100L), s.importedLeadIds());
    }
}
