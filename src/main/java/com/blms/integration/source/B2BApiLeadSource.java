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
 * GoF Adapter: turns the Apollo.io REST/JSON API into the LeadSource interface (java.net.http + Gson, D1).
 * Apollo (docs.apollo.io, 2026-11): People API Search {@code POST /mixed_people/api_search} finds people but never
 * returns emails, so a private second call to Bulk People Enrichment {@code POST /people/bulk_match} (max 10 per
 * call, 1 credit per revealed email) fills them in. To the caller it is still one fetchLeads() (CONFLICTS.md I-002).
 * Offline demo mode: when apiKey is blank it returns built-in sample prospects (no network), so the demo
 * cannot fail on Wi-Fi or API quota (CONFLICTS.md gap G12).
 */
public class B2BApiLeadSource implements LeadSource {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final long MAX_PAUSE_SECONDS = 10;
    private static final int ENRICH_BATCH = 10;                 // Apollo bulk_match limit per request
    private static final String ENRICH_PATH = "../people/bulk_match";
    private static final String ENRICH_QUERY = "?reveal_personal_emails=false&reveal_phone_number=false";

    private final String apiKey;
    private final String endpoint;
    private final IdealCustomerProfile icp;

    public B2BApiLeadSource(String apiKey, IdealCustomerProfile icp) {
        this(apiKey, "https://api.apollo.io/api/v1/mixed_people/api_search", icp);
    }

    public B2BApiLeadSource(String apiKey, String endpoint, IdealCustomerProfile icp) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.icp = icp;
    }

    /**
     * SD05 steps 10-15: toSearchQuery() -> HTTP POST /mixed_people/api_search(query) -> JSON people
     * -> (Adapter-internal) POST /people/bulk_match for their emails -> List&lt;LeadData&gt;.
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
        HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        String query = icp.toSearchQuery();                                       // SD05 steps 11-12
        URI searchUri = URI.create(endpoint + (endpoint.contains("?") ? "&" : "?") + query);
        JsonObject found = call(http, searchUri, "{}");                           // SD05 steps 13-14
        List<JsonObject> people = objects(found, "people");
        if (people.isEmpty()) {
            people = objects(found, "contacts");
        }
        List<LeadData> rows = new ArrayList<>();
        List<JsonObject> batch = new ArrayList<>();
        for (JsonObject person : people) {                                         // SD05 step 15
            if (email(person) != null || text(person, "id") == null) {
                rows.add(toLeadData(person));                // already has an email (or cannot be enriched)
            } else {
                batch.add(person);
                if (batch.size() == ENRICH_BATCH) {
                    rows.addAll(enrich(http, batch));
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            rows.addAll(enrich(http, batch));
        }
        return rows;
    }

    public String getEndpoint() { return endpoint; }
    public IdealCustomerProfile getIcp() { return icp; }

    // ------------------------------------------------------------------ private helpers

    /** Bulk People Enrichment for up to 10 search results; an unmatched (null) entry keeps the search data. */
    private List<LeadData> enrich(HttpClient http, List<JsonObject> batch) {
        JsonArray details = new JsonArray();
        for (JsonObject person : batch) {
            JsonObject d = new JsonObject();
            d.addProperty("id", text(person, "id"));
            details.add(d);
        }
        JsonObject body = new JsonObject();
        body.add("details", details);
        URI enrichUri = URI.create(URI.create(endpoint).resolve(ENRICH_PATH) + ENRICH_QUERY);
        JsonObject result = call(http, enrichUri, body.toString());
        JsonArray matches = array(result, "matches");
        List<LeadData> rows = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            JsonObject match = matchFor(matches, i, text(batch.get(i), "id"));
            rows.add(toLeadData(match != null ? match : batch.get(i)));
        }
        return rows;
    }

    /** Apollo returns matches in request order; the id check guards against a reordered answer. */
    private static JsonObject matchFor(JsonArray matches, int index, String id) {
        if (matches == null) {
            return null;
        }
        if (index < matches.size() && matches.get(index).isJsonObject()) {
            JsonObject m = matches.get(index).getAsJsonObject();
            if (id == null || id.equals(text(m, "id"))) {
                return m;
            }
        }
        for (JsonElement el : matches) {
            if (el != null && el.isJsonObject() && id != null && id.equals(text(el.getAsJsonObject(), "id"))) {
                return el.getAsJsonObject();
            }
        }
        return null;
    }

    /** POST + UC05 extensions (429 pause and one retry, bad key, other HTTP errors) -> parsed JSON object. */
    private JsonObject call(HttpClient http, URI uri, String jsonBody) {
        HttpResponse<String> response = post(http, uri, jsonBody);
        if (response.statusCode() == 429) {
            pause(response);                                                 // UC05: "pauses the operation"
            response = post(http, uri, jsonBody);
            if (response.statusCode() == 429) {
                throw new IntegrationException(
                        "API rate limit exceeded (HTTP 429) - discovery paused, please try again in a few minutes");
            }
        }
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new IntegrationException("The B2B API rejected the API key (HTTP " + status + ") - check b2b.apiKey"
                    + " and that the key may use People API Search and Bulk People Enrichment");
        }
        if (status < 200 || status >= 300) {
            throw new IntegrationException("The B2B API returned an error (HTTP " + status + ")");
        }
        try {
            JsonElement root = JsonParser.parseString(response.body() == null ? "" : response.body());
            if (!root.isJsonObject()) {
                throw new IntegrationException("Unexpected response from the B2B API");
            }
            return root.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IntegrationException("Unexpected response from the B2B API", e);
        }
    }

    private HttpResponse<String> post(HttpClient http, URI uri, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("x-api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
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

    /** Null-safe person JSON (search result or enrichment match) -> LeadData. */
    private LeadData toLeadData(JsonObject p) {
        String name = text(p, "name");
        if (name == null) {
            String first = text(p, "first_name");
            String last = text(p, "last_name");              // search results only carry last_name_obfuscated
            name = first == null && last == null ? null : ((first == null ? "" : first) + " "
                    + (last == null ? "" : last)).trim();
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
        JsonObject primaryPhone = org == null ? null : object(org, "primary_phone");
        if (phone == null && primaryPhone != null) {
            phone = text(primaryPhone, "sanitized_number");
            if (phone == null) {
                phone = text(primaryPhone, "number");
            }
        }
        JsonArray phones = array(p, "phone_numbers");
        if (phone == null && phones != null && !phones.isEmpty() && phones.get(0).isJsonObject()) {
            phone = text(phones.get(0).getAsJsonObject(), "sanitized_number");
        }
        return new LeadData(company, name, email(p), text(p, "title"),
                industry == null ? icp.getTargetIndustry() : industry, phone, null);
    }

    /** The person's email, or null when missing or replaced by a provider placeholder. */
    private static String email(JsonObject p) {
        String email = text(p, "email");
        return email == null || email.contains("not_unlocked") || !email.contains("@") ? null : email;
    }

    private static List<JsonObject> objects(JsonObject o, String key) {
        List<JsonObject> out = new ArrayList<>();
        JsonArray list = array(o, key);
        if (list != null) {
            for (JsonElement el : list) {
                if (el != null && el.isJsonObject()) {
                    out.add(el.getAsJsonObject());
                }
            }
        }
        return out;
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
