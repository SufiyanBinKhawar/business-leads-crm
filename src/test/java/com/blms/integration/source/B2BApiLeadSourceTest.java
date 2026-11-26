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

/** UC05 / SD05 steps 10-15 against a local HTTP server - no internet needed. */
class B2BApiLeadSourceTest {

    private final IdealCustomerProfile icp = new IdealCustomerProfile("Fintech", "Payments consulting");
    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> lastKey = new AtomicReference<>();

    private String serve(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/people/search", ex -> {
            hits.incrementAndGet();
            lastQuery.set(ex.getRequestURI().getRawQuery());
            lastKey.set(ex.getRequestHeaders().getFirst("X-Api-Key"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            if (status == 429) {
                ex.getResponseHeaders().add("Retry-After", "1");
            }
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/people/search";
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
    void parsesJsonNullSafelyAndSendsTheIcpQuery() throws IOException {
        String url = serve(200, """
                {"people":[
                  {"first_name":"Sara","last_name":"Malik","email":"sara@acme.example","title":"CTO",
                   "organization":{"name":"Acme","industry":"Fintech","phone":"+92 300"}},
                  {"name":"No Email","email":null,"organization":null},
                  {"name":"Hidden","email":"email_not_unlocked@domain.com","organization_name":"Secret Co"}
                ]}""");
        List<LeadData> rows = new B2BApiLeadSource("key-123", url, icp).fetchLeads();
        assertEquals("industry=Fintech&keywords=Payments+consulting", lastQuery.get());
        assertEquals("key-123", lastKey.get());
        assertEquals(3, rows.size());
        assertEquals("Sara Malik", rows.get(0).contactName());
        assertEquals("Acme", rows.get(0).companyName());
        assertTrue(rows.get(0).isValid());
        assertNull(rows.get(1).email());
        assertFalse(rows.get(1).isValid());
        assertNull(rows.get(2).email());                       // provider placeholder treated as missing
        assertEquals("Fintech", rows.get(2).industry());       // falls back to the ICP industry
    }

    @Test
    void uc05RateLimitPausesRetriesOnceThenNotifies() throws IOException {
        String url = serve(429, "{}");
        IntegrationException e = assertThrows(IntegrationException.class,
                () -> new B2BApiLeadSource("key", url, icp).fetchLeads());
        assertTrue(e.getMessage().contains("rate limit"));
        assertTrue(e.getMessage().contains("HTTP 429"));
        assertEquals(2, hits.get());
    }

    @Test
    void uc05NetworkFailureAsksToCheckTheConnection() throws IOException {
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        IntegrationException e = assertThrows(IntegrationException.class,
                () -> new B2BApiLeadSource("key", "http://127.0.0.1:" + closedPort + "/x", icp).fetchLeads());
        assertTrue(e.getMessage().contains("internet connection"));
    }

    @Test
    void badKeyAndGarbageAreIntegrationErrors() throws IOException {
        String url = serve(401, "nope");
        assertTrue(assertThrows(IntegrationException.class,
                () -> new B2BApiLeadSource("key", url, icp).fetchLeads()).getMessage().contains("HTTP 401"));
        server.stop(0);
        String url2 = serve(200, "<html>not json</html>");
        assertThrows(IntegrationException.class, () -> new B2BApiLeadSource("key", url2, icp).fetchLeads());
    }
}
