package com.blms.service;

import com.blms.domain.Lead;
import com.blms.domain.Session;
import com.blms.domain.Workspace;
import com.blms.dto.ImportSummary;
import com.blms.dto.LeadData;
import com.blms.exception.AuthenticationException;
import com.blms.integration.source.LeadSource;
import com.blms.persistence.LeadRepository;
import com.blms.util.Validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * D5 Figure 3 «Pure Fabrication» LeadImportService (+leads LeadRepository, «use» Workspace).
 * ONE import algorithm for UC04 and UC05 (D1 "dual ingestion without code duplication"): it only sees the
 * LeadSource interface, so fetchLeads() is bound at run time to CsvLeadSource or B2BApiLeadSource (Polymorphism).
 */
public class LeadImportService {

    private final LeadRepository leads;
    /** Current session - supplies the +creator of new leads (CONFLICTS.md gap G5). */
    private final Supplier<Session> session;

    public LeadImportService(LeadRepository leads, Supplier<Session> session) {
        this.leads = leads;
        this.session = session;
    }

    /** SD04 steps 8-17 / SD05 steps 9-22. */
    public ImportSummary importFrom(LeadSource source, Workspace ws) {
        Session current = session.get();
        if (current == null) {
            throw new AuthenticationException("Please log in first");
        }
        List<Lead> newLeads = new ArrayList<>();
        Set<String> domainsInThisRun = new HashSet<>();
        int duplicates = 0;
        int invalid = 0;
        for (LeadData row : source.fetchLeads()) {
            if (!row.isValid()) {
                invalid++;
                continue;
            }
            String domain = Validator.emailDomain(row.email());
            if (!domainsInThisRun.add(domain) || leads.existsByEmailDomain(row.email())) {
                duplicates++;
                continue;
            }
            newLeads.add(ws.createLead(row.withCreator(current.getUser())));
        }
        int imported = newLeads.isEmpty() ? 0 : leads.saveAll(newLeads);
        List<Long> ids = newLeads.stream().map(Lead::getLeadId).toList();
        return new ImportSummary(imported, duplicates, invalid, ids);
    }
}
