package com.blms.controller;

import com.blms.TestData;
import com.blms.domain.Lead;
import com.blms.domain.PipelineStatus;
import com.blms.dto.ExportCriteria;
import com.blms.dto.LeadData;
import com.blms.dto.LeadFilter;
import com.blms.exception.ValidationException;
import com.blms.integration.export.CsvExporter;
import com.blms.persistence.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC12 / SD12 with an in-memory LeadRepository and the real CsvExporter. */
class ExportControllerTest {

    @TempDir
    Path dir;

    private final List<Lead> rows = new ArrayList<>();
    private ExportController controller;

    @BeforeEach
    void setUp() {
        LeadRepository repo = new LeadRepository(null) {
            @Override
            public List<Lead> findByFilter(LeadFilter f) {
                return rows.stream().filter(l -> l.matches(f)).toList();
            }
        };
        controller = new ExportController(repo, new CsvExporter());
        Lead a = TestData.workspace().createLead(new LeadData("Blue Lake Foods, Ltd", "Omar \"OJ\" Farooq",
                "omar@bluelake.example", "Head, Procurement", "Food", null, TestData.rep()));
        a.setLeadId(1L);
        Lead b = TestData.lead();
        b.setLeadId(2L);
        b.changeStatus(PipelineStatus.QUALIFIED);
        rows.add(a);
        rows.add(b);
    }

    @Test
    void exportsAllLeadsWithEscapedFields() throws Exception {
        controller.requestExport(ExportCriteria.allLeads());
        Path out = dir.resolve("all.csv");
        assertEquals(2, controller.chooseDestination(out.toString()));
        List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertTrue(lines.get(1).contains("\"Blue Lake Foods, Ltd\""));
        assertTrue(lines.get(1).contains("\"Omar \"\"OJ\"\" Farooq\""));
        assertTrue(lines.get(1).contains("\"Head, Procurement\""));
    }

    @Test
    void exportsOnlyTheChosenStatus() throws Exception {
        controller.requestExport(ExportCriteria.only(PipelineStatus.QUALIFIED));
        assertEquals(1, controller.chooseDestination(dir.resolve("q.csv").toString()));
    }

    @Test
    void uc12NoRecordsMatchAbortsWithoutAFile() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> controller.requestExport(ExportCriteria.only(PipelineStatus.CLOSED_WON)));
        assertTrue(e.getMessage().contains("empty"));
        Path out = dir.resolve("none.csv");
        assertThrows(ValidationException.class, () -> controller.chooseDestination(out.toString()));
        assertFalse(Files.exists(out));
    }

    @Test
    void uc12WriteDeniedPromptsForAnotherFolderAndCanRetry() throws Exception {
        controller.requestExport(ExportCriteria.allLeads());
        Path folderNotFile = Files.createDirectory(dir.resolve("a-folder"));
        ValidationException e = assertThrows(ValidationException.class,
                () -> controller.chooseDestination(folderNotFile.toString()));
        assertTrue(e.getMessage().contains("choose another folder"));
        assertEquals(2, controller.chooseDestination(dir.resolve("retry.csv").toString()));
    }
}
