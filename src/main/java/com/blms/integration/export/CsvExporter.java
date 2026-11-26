package com.blms.integration.export;

import com.blms.domain.Contact;
import com.blms.domain.Lead;
import com.blms.exception.ValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * D5 Figure 3 CsvExporter realizes Exporter (UC12). Quotes any field that contains a comma (SD12 note) - in fact
 * every text field is wrapped in double quotes and inner quotes are doubled (RFC 4180), so commas, quotes and line
 * breaks inside a field never break the columns (D3 UC12 special requirement).
 */
public class CsvExporter implements Exporter {

    static final String HEADER =
            "Lead ID,Company,Industry,Contact,Email,Job Role,Phone,Status,Archived,Created At";

    /** SD12 step 6: header row + one row per lead. */
    @Override
    public List<String> format(List<Lead> leads) {
        List<String> rows = new ArrayList<>();
        rows.add(HEADER);
        for (Lead lead : leads) {
            Contact c = lead.getContacts().isEmpty() ? null : lead.getPrimaryContact();
            rows.add(String.join(",",
                    lead.getLeadId() == null ? "" : String.valueOf(lead.getLeadId()),
                    quote(lead.getCompanyName()),
                    quote(lead.getIndustry()),
                    quote(c == null ? null : c.getFullName()),
                    quote(c == null ? null : c.getEmail()),
                    quote(c == null ? null : c.getJobRole()),
                    quote(lead.getPhone() != null ? lead.getPhone() : c == null ? null : c.getPhone()),
                    lead.getStatus().name(),
                    lead.isArchived() ? "yes" : "no",
                    lead.getCreatedAt() == null ? "" : lead.getCreatedAt().withNano(0).toString()));
        }
        return rows;
    }

    /** SD12 step 11. UTF-8 file. Disk write denied -> ValidationException("Cannot write there - choose another folder"). */
    @Override
    public void write(List<String> rows, String path) {
        if (path == null || path.isBlank()) {
            throw new ValidationException("Choose where to save the CSV file");
        }
        try {
            Files.write(Path.of(path), rows, StandardCharsets.UTF_8);
        } catch (IOException | InvalidPathException | SecurityException e) {
            throw new ValidationException("Cannot write there - choose another folder", e);
        }
    }

    private static String quote(String value) {
        return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
