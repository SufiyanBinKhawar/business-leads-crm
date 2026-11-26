package com.blms.integration.source;

import com.blms.dto.LeadData;
import com.blms.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC04 / SD04 steps 3-10: CSV parsing, column mapping and both D3 extensions. */
class CsvLeadSourceTest {

    @TempDir
    Path dir;

    private File write(String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toFile();
    }

    @Test
    void readsHeadersOfTheBundledSample() {
        File sample = new File("src/main/resources/sample/leads_sample.csv");
        assertArrayEquals(new String[] {"Company", "Contact Name", "Email Address", "Job Title", "Industry", "Phone"},
                new CsvLeadSource(sample).readHeaders());
    }

    @Test
    void mapsColumnsAndKeepsQuotedCommas() {
        CsvLeadSource src = new CsvLeadSource(new File("src/main/resources/sample/leads_sample.csv"));
        src.readHeaders();
        src.setColumnMap(Map.of("companyName", "Company", "contactName", "Contact Name", "email", "Email Address",
                "jobRole", "Job Title", "industry", "Industry", "phone", "Phone"));
        List<LeadData> rows = src.fetchLeads();
        assertEquals(5, rows.size());
        assertEquals("Blue Lake Foods, Ltd", rows.get(1).companyName());
        assertEquals("omar@bluelake.example", rows.get(1).email());
        assertNull(rows.get(2).phone());                      // blank cell -> null (null-safe)
        assertFalse(rows.get(4).isValid());                   // "Broken Row Inc" - irregular row
    }

    @Test
    void unmappedFieldsAreMatchedByHeaderName() throws IOException {
        File f = write("leads.csv", "Organization,Full Name,E-mail,Title\nAcme,Sara Malik,sara@acme.example,CTO\n");
        CsvLeadSource src = new CsvLeadSource(f);
        src.setColumnMap(Map.of());
        LeadData row = src.fetchLeads().get(0);
        assertEquals("Acme", row.companyName());
        assertEquals("Sara Malik", row.contactName());
        assertEquals("sara@acme.example", row.email());
        assertEquals("CTO", row.jobRole());
    }

    @Test
    void quotedLineBreaksAndEscapedQuotesAreOneField() throws IOException {
        File f = write("q.csv", "Company,Contact,Email\r\n\"Acme \"\"Robots\"\"\nHQ\",Sara,sara@acme.example\r\n");
        LeadData row = new CsvLeadSource(f).fetchLeads().get(0);
        assertEquals("Acme \"Robots\"\nHQ", row.companyName());
    }

    @Test
    void rowWithWrongColumnCountIsInvalidNotFatal() throws IOException {
        File f = write("r.csv", "Company,Contact,Email\nAcme,Sara,sara@acme.example\nBroken,row\n");
        List<LeadData> rows = new CsvLeadSource(f).fetchLeads();
        assertEquals(2, rows.size());
        assertTrue(rows.get(0).isValid());
        assertFalse(rows.get(1).isValid());
    }

    @Test
    void uc04UnsupportedFileTypeIsRejected() throws IOException {
        File txt = write("leads.txt", "Company,Contact,Email\n");
        ValidationException e = assertThrows(ValidationException.class, () -> new CsvLeadSource(txt).readHeaders());
        assertTrue(e.getMessage().contains("UTF-8"));
    }

    @Test
    void uc04NonUtf8FileIsRejected() throws IOException {
        Path p = dir.resolve("latin1.csv");
        Files.write(p, new byte[] {'C', 'o', ',', 'E', '\n', (byte) 0xE9, ',', 'x', '\n'});
        assertThrows(ValidationException.class, () -> new CsvLeadSource(p.toFile()).readHeaders());
    }

    @Test
    void uc04EntirelyMalformedFileIsRejectedBeforeAnyImport() throws IOException {
        assertThrows(ValidationException.class, () -> new CsvLeadSource(write("empty.csv", "  \n")).readHeaders());
        assertThrows(ValidationException.class,
                () -> new CsvLeadSource(write("noRows.csv", "Company,Contact,Email\n")).fetchLeads());
        assertThrows(ValidationException.class,
                () -> new CsvLeadSource(write("open.csv", "Company,Contact,Email\n\"Acme,Sara,s@a.example\n")).fetchLeads());
        assertThrows(ValidationException.class,
                () -> new CsvLeadSource(write("shape.csv", "Company,Contact,Email\na,b\nc\n")).fetchLeads());
    }

    @Test
    void missingRequiredColumnIsReported() throws IOException {
        File f = write("m.csv", "Company,Notes\nAcme,hello\n");
        ValidationException e = assertThrows(ValidationException.class, () -> new CsvLeadSource(f).fetchLeads());
        assertTrue(e.getMessage().contains("contact name"));
        assertTrue(e.getMessage().contains("email"));
    }
}
