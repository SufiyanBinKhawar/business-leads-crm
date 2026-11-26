package com.blms.integration.source;

import com.blms.dto.LeadData;
import com.blms.exception.ValidationException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * D5 Figure 3 CsvLeadSource realizes LeadSource (UC04). Created by LeadIngestionController (SD04 step 2).
 * columnMap keys are the system field names: companyName, contactName, email, jobRole, industry, phone;
 * values are the CSV header chosen by the actor. A field the actor left unmapped is matched to a header with the
 * same meaning ("Email Address" -> email), so a mapping only has to be confirmed, not typed.
 * Parsing follows RFC 4180: commas and line breaks inside double quotes, "" for a literal quote.
 */
public class CsvLeadSource implements LeadSource {

    /** UC04 extension "Unsupported file type: System rejects the file and requests a UTF-8 CSV". */
    static final String UNSUPPORTED = "Unsupported file - please choose a UTF-8 encoded .csv file";
    /** UC04 extension "Entire file is malformed: System aborts the process ... before any database changes". */
    static final String MALFORMED = "The CSV file is malformed - the import was aborted and nothing was saved";

    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();

    static {
        SYNONYMS.put("companyName", List.of("companyname", "company", "organization", "organisation", "account",
                "business", "companyorganization"));
        SYNONYMS.put("contactName", List.of("contactname", "contact", "name", "fullname", "person", "contactperson"));
        SYNONYMS.put("email", List.of("email", "emailaddress", "mail", "workemail", "contactemail"));
        SYNONYMS.put("jobRole", List.of("jobrole", "jobtitle", "title", "role", "position", "designation"));
        SYNONYMS.put("industry", List.of("industry", "sector", "vertical"));
        SYNONYMS.put("phone", List.of("phone", "phonenumber", "telephone", "mobile", "tel", "contactnumber"));
    }

    private final File file;
    private Map<String, String> columnMap = new HashMap<>();

    public CsvLeadSource(File file) {
        this.file = file;
    }

    /**
     * SD04 steps 3-4. UC04 extensions: not a .csv / not UTF-8 -> ValidationException("... UTF-8 ... .csv file");
     * empty or unreadable file -> ValidationException before any database change.
     */
    public String[] readHeaders() {
        List<List<String>> records = readRecords();
        List<String> header = records.get(0);
        if (header.stream().allMatch(String::isBlank)) {
            throw new ValidationException(MALFORMED);
        }
        return header.toArray(new String[0]);
    }

    /** SD04 step 7. */
    public void setColumnMap(Map<String, String> map) {
        this.columnMap = map == null ? new HashMap<>() : new HashMap<>(map);
    }

    /**
     * SD04 steps 9-10: one LeadData per data row; malformed rows are returned too (isValid() == false).
     * The whole file is rejected (nothing returned, so nothing saved) when the required columns cannot be found,
     * when there are no data rows, or when no row at all has the header's shape.
     */
    @Override
    public List<LeadData> fetchLeads() {
        List<List<String>> records = readRecords();
        List<String> header = records.get(0);
        Map<String, Integer> index = resolveColumns(header);

        List<LeadData> rows = new ArrayList<>();
        int wellFormed = 0;
        for (int r = 1; r < records.size(); r++) {
            List<String> rec = records.get(r);
            if (rec.size() == 1 && rec.get(0).isBlank()) {
                continue;                                     // blank line
            }
            if (rec.size() != header.size()) {
                rows.add(new LeadData(null, null, null, null, null, null, null));   // irregular row (UC04 step 4)
                continue;
            }
            wellFormed++;
            rows.add(new LeadData(cell(rec, index, "companyName"), cell(rec, index, "contactName"),
                    cell(rec, index, "email"), cell(rec, index, "jobRole"), cell(rec, index, "industry"),
                    cell(rec, index, "phone"), null));
        }
        if (rows.isEmpty() || wellFormed == 0) {
            throw new ValidationException(MALFORMED);
        }
        return rows;
    }

    public File getFile() { return file; }

    // ------------------------------------------------------------------ private helpers

    /** Reads and parses the whole file as strict UTF-8; at least a header record is guaranteed. */
    private List<List<String>> readRecords() {
        if (file == null || !file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ValidationException(UNSUPPORTED);
        }
        String text;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new ValidationException(UNSUPPORTED, e);
        } catch (IOException e) {
            throw new ValidationException("Cannot read the file - " + MALFORMED, e);
        }
        if (text.startsWith("﻿")) {
            text = text.substring(1);                         // UTF-8 byte-order mark written by Excel
        }
        if (text.isBlank()) {
            throw new ValidationException(MALFORMED);
        }
        return parse(text);
    }

    /** RFC 4180 parser. An unterminated quote means the whole file is malformed. */
    private static List<List<String>> parse(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                current.add(field.toString());
                field.setLength(0);
            } else if (ch == '\r' || ch == '\n') {
                current.add(field.toString());
                field.setLength(0);
                records.add(current);
                current = new ArrayList<>();
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                field.append(ch);
            }
            i++;
        }
        if (quoted) {
            throw new ValidationException(MALFORMED);
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            records.add(current);
        }
        return records;
    }

    /** System field -> column index, from the actor's mapping first and the header names second. */
    private Map<String, Integer> resolveColumns(List<String> header) {
        Map<String, Integer> index = new HashMap<>();
        for (String fieldName : SYNONYMS.keySet()) {
            String chosen = columnMap.get(fieldName);
            if (chosen != null && !chosen.isBlank()) {
                int at = indexOfHeader(header, chosen);
                if (at < 0) {
                    throw new ValidationException("Column '" + chosen + "' is not in the CSV file");
                }
                index.put(fieldName, at);
            }
        }
        for (Map.Entry<String, List<String>> e : SYNONYMS.entrySet()) {
            if (index.containsKey(e.getKey())) {
                continue;
            }
            for (int h = 0; h < header.size(); h++) {
                if (!index.containsValue(h) && e.getValue().contains(normalize(header.get(h)))) {
                    index.put(e.getKey(), h);
                    break;
                }
            }
        }
        List<String> missing = new ArrayList<>();
        if (!index.containsKey("companyName")) {
            missing.add("company name");
        }
        if (!index.containsKey("contactName")) {
            missing.add("contact name");
        }
        if (!index.containsKey("email")) {
            missing.add("email");
        }
        if (!missing.isEmpty()) {
            throw new ValidationException("Map a CSV column to: " + String.join(", ", missing));
        }
        return index;
    }

    private static int indexOfHeader(List<String> header, String name) {
        for (int h = 0; h < header.size(); h++) {
            if (header.get(h).trim().equalsIgnoreCase(name.trim())) {
                return h;
            }
        }
        return -1;
    }

    private static String normalize(String headerName) {
        return headerName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Null-safe cell read: unmapped column or blank cell -> null. */
    private static String cell(List<String> rec, Map<String, Integer> index, String fieldName) {
        Integer at = index.get(fieldName);
        if (at == null) {
            return null;
        }
        String value = rec.get(at).trim();
        return value.isEmpty() ? null : value;
    }
}
