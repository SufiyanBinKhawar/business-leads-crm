package com.blms.integration.source;

import com.blms.domain.IdealCustomerProfile;
import com.blms.dto.LeadData;
import com.blms.exception.IntegrationException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC05 / SD05 steps 10-15 against a local HTTP server shaped like Apollo (People API Search + Bulk People
 * Enrichment, CONFLICTS.md I-002) - no internet needed.
 */
class B2BApiLeadSourceTest {

    private static final String SEARCH = "/api/v1/mixed_people/api_search";
    private static final String ENRICH = "/api/v1/people/bulk_match";

    private final IdealCustomerProfile icp = new IdealCustomerProfile("Fintech", "Payments consulting");
    private HttpServer server;
    private final AtomicInteger searchHits = new AtomicInteger();
    private final AtomicInteger enrichHits = new AtomicInteger();
    private final AtomicReference<String> searchMethod = new AtomicReference<>();
    private final AtomicReference<String> searchQuery = new AtomicReference<>();
    private final AtomicReference<String> searchKey = new AtomicReference<>();
    private final AtomicReference<String> enrichQuery = new AtomicReference<>();
    private final AtomicReference<String> enrichBody = new AtomicReference<>();

    /** Starts the fake Apollo; returns the search endpoint URL. */
    private String serve(int searchStatus, String searchBody, int enrichStatus, String enrichResponse)
            throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(SEARCH, ex -> {
            searchHits.incrementAndGet();
            searchMethod.set(ex.getRequestMethod());
            searchQuery.set(ex.getRequestURI().getRawQuery());
            searchKey.set(ex.getRequestHeaders().getFirst("x-api-key"));
            ex.getRequestBody().readAllBytes();
            reply(ex, searchStatus, searchBody);
        });
        server.createContext(ENRICH, ex -> {
            enrichHits.incrementAndGet();
            enrichQuery.set(ex.getRequestURI().getRawQuery());
            enrichBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(ex, enrichStatus, enrichResponse);
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + SEARCH;
    }

    private static void reply(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (status == 429) {
            ex.getResponseHeaders().add("Retry-After", "1");
        }
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void offlineModeNeedsNoNetwork() {
        List<LeadData> rows = new B2BApiLeadSource("", icp).fetchLeads();
        assertEquals(3, rows.size());
        assertFalse(rows.get(2).isValid());                  // contact name missing -> reported invalid
    }

    @Test
    void searchesThenEnrichesEmailsNullSafely() throws IOException {
        String url = serve(200, """
                {"total_entries":3,"people":[
                  {"id":"p1","first_name":"Sara","last_name_obfuscated":"Ma***k","title":"CTO",
                   "organization":{"name":"Acme","has_industry":true}},
                  {"id":"p2","first_name":"Omar","last_name_obfuscated":"Fa***q","organization":{"name":"Blue Lake"}},
                  {"id":"p3","first_name":"Hidden","organization":{"name":"Secret Co"}}
                ]}""", 200, """
                {"status":"success","matches":[
                  {"id":"p1","first_name":"Sara","last_name":"Malik","email":"sara@acme.example","title":"CTO",
                   "organization":{"name":"Acme","industry":"financial services","phone":"+92 300"}},
                  null,
                  {"id":"p3","first_name":"Hidden","email":"email_not_unlocked@domain.com",
                   "organization":{"name":"Secret Co","primary_phone":{"sanitized_number":"+923001234567"}}}
                ]}""");
        List<LeadData> rows = new B2BApiLeadSource("key-123", url, icp).fetchLeads();

        assertEquals("POST", searchMethod.get());
        assertEquals("q_keywords=Fintech&per_page=10&page=1", searchQuery.get());
        assertEquals("key-123", searchKey.get());
        assertEquals(1, enrichHits.get());                      // 3 people -> one bulk_match call
        assertEquals("reveal_personal_emails=false&reveal_phone_number=false", enrichQuery.get());
        assertEquals("{\"details\":[{\"id\":\"p1\"},{\"id\":\"p2\"},{\"id\":\"p3\"}]}", enrichBody.get());

        assertEquals(3, rows.size());
        assertEquals("Sara Malik", rows.get(0).contactName());
        assertEquals("sara@acme.example", rows.get(0).email());
        assertEquals("Acme", rows.get(0).companyName());
        assertEquals("financial services", rows.get(0).industry());
        assertEquals("+92 300", rows.get(0).phone());
        assertTrue(rows.get(0).isValid());
        assertEquals("Blue Lake", rows.get(1).companyName());   // unmatched (null) -> search data, no email
        assertNull(rows.get(1).email());
        assertFalse(rows.get(1).isValid());
        assertNull(rows.get(2).email());                        // provider placeholder treated as missing
        assertEquals("+923001234567", rows.get(2).phone());
        assertEquals("Fintech", rows.get(2).industry());        // falls back to the ICP industry
    }

    @Test
    void elevenPeopleNeedTwoEnrichmentCallsAndEmptySearchNone() throws IOException {
        StringBuilder people = new StringBuilder();
        for (int i = 1; i <= 11; i++) {
            people.append(i == 1 ? "" : ",").append("{\"id\":\"p").append(i).append("\",\"first_name\":\"N\"}");
        }
        String url = serve(200, "{\"people\":[" + people + "]}", 200, "{\"matches\":[]}");
        assertEquals(11, new B2BApiLeadSource("key", url, icp).fetchLeads().size());
        assertEquals(2, enrichHits.get());                      // Apollo limit: 10 per bulk_match
        server.stop(0);

        enrichHits.set(0);
        String url2 = serve(200, "{\"total_entries\":0,\"people\":[]}", 200, "{}");
        assertTrue(new B2BApiLeadSource("key", url2, icp).fetchLeads().isEmpty());
        assertEquals(0, enrichHits.get());
    }

    @Test
    void peopleThatAlreadyHaveAnEmailAreNotEnriched() throws IOException {
        String url = serve(200, """
                {"people":[{"id":"p1","name":"Sara Malik","email":"sara@acme.example",
                            "organization":{"name":"Acme"}}]}""", 500, "boom");
        List<LeadData> rows = new B2BApiLeadSource("key", url, icp).fetchLeads();
        assertEquals(0, enrichHits.get());
        assertTrue(rows.get(0).isValid());
    }

    @Test
    void uc05RateLimitPausesRetriesOnceThenNotifies() throws IOException {
        String url = serve(429, "{}", 200, "{}");
        IntegrationException e = assertThrows(IntegrationException.class,
                () -> new B2BApiLeadSource("key", url, icp).fetchLeads());
        assertTrue(e.getMessage().contains("rate limit"));
        assertTrue(e.getMessage().contains("HTTP 429"));
        assertEquals(2, searchHits.get());
    }

    @Test
    void uc05NetworkFailureAsksToCheckTheConnection() throws IOException {
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        IntegrationException e = assertThrows(IntegrationException.class,
                () -> new B2BApiLeadSource("key", "http://127.0.0.1:" + closedPort + SEARCH, icp).fetchLeads());
        assertTrue(e.getMessage().contains("internet connection"));
    }

    @Test
    void badKeyGarbageAndBlockedEnrichmentAreIntegrationErrors() throws IOException {
        String url = serve(401, "nope", 200, "{}");
        assertTrue(assertThrows(IntegrationException.class,
                () -> new B2BApiLeadSource("key", url, icp).fetchLeads()).getMessage().contains("HTTP 401"));
        server.stop(0);

        String url2 = serve(200, "<html>not json</html>", 200, "{}");
        assertThrows(IntegrationException.class, () -> new B2BApiLeadSource("key", url2, icp).fetchLeads());
        server.stop(0);

        String url3 = serve(200, "{\"people\":[{\"id\":\"p1\",\"first_name\":\"N\"}]}", 403, "{}");
        IntegrationException e = assertThrows(IntegrationException.class,
                () -> new B2BApiLeadSource("key", url3, icp).fetchLeads());
        assertTrue(e.getMessage().contains("HTTP 403"));          // scoped key without Bulk People Enrichment
        assertTrue(e.getMessage().contains("Bulk People Enrichment"));
    }
}
