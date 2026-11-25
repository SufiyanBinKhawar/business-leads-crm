package com.blms.integration.export;

import com.blms.domain.Lead;

import java.util.List;

/** D5 Figure 3 CsvExporter realizes Exporter (UC12). Quotes any field that contains a comma (SD12 note). */
public class CsvExporter implements Exporter {

    @Override
    public List<String> format(List<Lead> leads) {
        // TODO(Sufiyan, UC12): header row + one row per lead: id, company, industry, contact, email, role, phone,
        //  status, createdAt. Wrap every text field in double quotes and double any inner quotes.
        throw new UnsupportedOperationException("TODO UC12 CsvExporter.format");
    }

    @Override
    public void write(List<String> rows, String path) {
        // TODO(Sufiyan, UC12): Files.write(Path.of(path), rows, UTF_8); IOException -> ValidationException
        throw new UnsupportedOperationException("TODO UC12 CsvExporter.write");
    }
}
