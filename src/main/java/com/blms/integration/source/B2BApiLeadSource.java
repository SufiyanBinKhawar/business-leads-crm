package com.blms.integration.source;

import com.blms.domain.IdealCustomerProfile;
import com.blms.dto.LeadData;

import java.util.List;

/**
 * D5 Figure 3 B2BApiLeadSource realizes LeadSource (UC05), +icp 1 IdealCustomerProfile.
 * Created by LeadIngestionController with (apiKey, icp) - SD05 step 4.
 * Offline demo mode: when apiKey is blank it returns built-in sample prospects (no network), so the demo
 * cannot fail on Wi-Fi or API quota (CONFLICTS.md gap G12).
 */
public class B2BApiLeadSource implements LeadSource {

    private final String apiKey;
    private final String endpoint;
    private final IdealCustomerProfile icp;

    public B2BApiLeadSource(String apiKey, IdealCustomerProfile icp) {
        this(apiKey, "https://api.apollo.io/api/v1/mixed_people/search", icp);
    }

    public B2BApiLeadSource(String apiKey, String endpoint, IdealCustomerProfile icp) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.icp = icp;
    }

    /** SD05 steps 10-15. */
    @Override
    public List<LeadData> fetchLeads() {
        if (apiKey == null || apiKey.isBlank()) {
            String industry = icp.getTargetIndustry();
            return List.of(
                    new LeadData("Orbit Analytics", "Bilal Khan", "bilal@orbit.example", "Head of Ops", industry, null, null),
                    new LeadData("Vertex Labs", "Ayesha Raza", "ayesha@vertexlabs.example", "CTO", industry, null, null),
                    new LeadData("Quanta Retail", null, "info@quanta.example", null, industry, null, null));
        }
        // TODO(Sufiyan, UC05): String query = icp.toSearchQuery(); HTTP GET endpoint?query with java.net.http.HttpClient
        //  and header X-Api-Key: apiKey; parse JSON with Gson null-safely into LeadData.
        //  HTTP 429 -> IntegrationException("API rate limit exceeded - discovery paused")
        //  HttpTimeoutException / IOException -> IntegrationException("Check your internet connection")
        throw new UnsupportedOperationException("TODO UC05 B2BApiLeadSource.fetchLeads");
    }

    public String getEndpoint() { return endpoint; }
    public IdealCustomerProfile getIcp() { return icp; }
}
