package com.blms.integration.source;

import com.blms.domain.IdealCustomerProfile;
import com.blms.dto.LeadData;
import com.blms.exception.IntegrationException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * D5 Figure 3 B2BApiLeadSource realizes LeadSource (UC05), +icp 1 IdealCustomerProfile.
 * Created by LeadIngestionController with (apiKey, icp) - SD05 step 4.
 * GoF Adapter: turns the provider's REST/JSON API into the LeadSource interface (java.net.http + Gson, D1).
 * Offline demo mode: when apiKey is blank it returns built-in sample prospects (no network), so the demo
 * cannot fail on Wi-Fi or API quota (CONFLICTS.md gap G12).
 */
public class B2BApiLeadSource implements LeadSource {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final long MAX_PAUSE_SECONDS = 10;

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

    /**
     * SD05 steps 10-15: toSearchQuery() -> HTTP GET /people/search(query) -> JSON -> List&lt;LeadData&gt;.
     * UC05 extensions: HTTP 429 -> pause (Retry-After, max 10 s) and retry once, then
     * IntegrationException("API rate limit exceeded ..."); timeout / no network -> IntegrationException asking the
     * user to check the internet connection (the fetch is aborted). D3 special requirement: missing contact fields
     * in the JSON become null (the row is then reported as invalid, never a crash).
     */
    @Override
    public List<LeadData> fetchLeads() {
        if (apiKey == null || apiKey.isBlank()) {
            String industry = icp.getTargetIndustry();
            return List.of(
                    new LeadData("Orbit Analytics", "Bilal Khan", "bilal@orbit.example", "Head of Ops", industry, null, null),
                    new LeadData("Vertex Labs", "Ayesha Raza", "ayesha@vertexlabs.example", "CTO", industry, null, null),
                    new LeadData("Quanta Retail", null, "info@quanta.example", null, industry, null, null));
        }
        String query = icp.toSearchQuery();                                  // SD05 steps 11-12
        HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        HttpResponse<String> response = get(http, query);                   // SD05 steps 13-14
        if (response.statusCode() == 429) {
            pause(response);                                                 // UC05: "pauses the operation"
            response = get(http, query);
            if (response.statusCode() == 429) {
                throw new IntegrationException(
                        "API rate limit exceeded (HTTP 429) - discovery paused, please try again in a few minutes");
            }
        }
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new IntegrationException("The B2B API rejected the API key (HTTP " + status + ") - check b2b.apiKey");
        }
        if (status < 200 || status >= 300) {
            throw new IntegrationException("The B2B API returned an error (HTTP " + status + ")");
        }
        return parse(response.body());                                       // SD05 step 15
    }

    public String getEndpoint() { return endpoint; }
    public IdealCustomerProfile getIcp() { return icp; }

    // ------------------------------------------------------------------ private helpers

    private HttpResponse<String> get(HttpClient http, String query) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + (endpoint.contains("?") ? "&" : "?") + query))
                .timeout(REQUEST_TIMEOUT)
                .header("X-Api-Key", apiKey)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .GET()
                .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new IntegrationException("Network timeout - check your internet connection (the fetch was aborted)", e);
        } catch (IOException e) {
            throw new IntegrationException("Cannot reach the B2B API - check your internet connection", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("Lead discovery was cancelled", e);
        }
    }

    /** Waits for the provider's Retry-After (seconds, capped) before the single retry. */
    private static void pause(HttpResponse<String> response) {
        long seconds = response.headers().firstValue("Retry-After").map(v -> {
            try {
                return Long.parseLong(v.trim());
            } catch (NumberFormatException e) {
                return MAX_PAUSE_SECONDS;
            }
        }).orElse(MAX_PAUSE_SECONDS);
        try {
            Thread.sleep(Duration.ofSeconds(Math.max(1, Math.min(seconds, MAX_PAUSE_SECONDS))));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("Lead discovery was cancelled", e);
        }
    }

    /** Null-safe JSON -> LeadData (Apollo "people"/"contacts"; also accepts "results"). */
    private List<LeadData> parse(String body) {
        List<LeadData> rows = new ArrayList<>();
        try {
            JsonElement rootEl = JsonParser.parseString(body == null ? "" : body);
            if (!rootEl.isJsonObject()) {
                throw new IntegrationException("Unexpected response from the B2B API");
            }
            JsonObject root = rootEl.getAsJsonObject();
            for (String key : List.of("people", "contacts", "results")) {
                JsonArray list = array(root, key);
                if (list == null) {
                    continue;
                }
                for (JsonElement el : list) {
                    if (el != null && el.isJsonObject()) {
                        rows.add(toLeadData(el.getAsJsonObject()));
                    }
                }
            }
        } catch (JsonParseException | IllegalStateException e) {
            throw new IntegrationException("Unexpected response from the B2B API", e);
        }
        return rows;
    }

    private LeadData toLeadData(JsonObject p) {
        String name = text(p, "name");
        if (name == null) {
            String first = text(p, "first_name");
            String last = text(p, "last_name");
            name = first == null && last == null ? null : ((first == null ? "" : first) + " "
                    + (last == null ? "" : last)).trim();
        }
        String email = text(p, "email");
        if (email != null && email.contains("not_unlocked")) {
            email = null;                                    // provider placeholder for a hidden address
        }
        JsonObject org = object(p, "organization");
        if (org == null) {
            org = object(p, "account");
        }
        String company = org == null ? null : text(org, "name");
        if (company == null) {
            company = text(p, "organization_name");
        }
        String industry = org == null ? null : text(org, "industry");
        String phone = org == null ? null : text(org, "phone");
        JsonArray phones = array(p, "phone_numbers");
        if (phone == null && phones != null && !phones.isEmpty() && phones.get(0).isJsonObject()) {
            phone = text(phones.get(0).getAsJsonObject(), "sanitized_number");
        }
        return new LeadData(company, name, email, text(p, "title"),
                industry == null ? icp.getTargetIndustry() : industry, phone, null);
    }

    private static String text(JsonObject o, String key) {
        JsonElement v = o.get(key);
        if (v == null || v.isJsonNull() || !v.isJsonPrimitive()) {
            return null;
        }
        String s = v.getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    private static JsonObject object(JsonObject o, String key) {
        JsonElement v = o.get(key);
        return v != null && v.isJsonObject() ? v.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject o, String key) {
        JsonElement v = o.get(key);
        return v != null && v.isJsonArray() ? v.getAsJsonArray() : null;
    }
}
