package com.blms.integration.export;

import com.blms.domain.Lead;

import java.util.List;

/** D5 Figure 3 «interface» Exporter (Polymorphism, Protected Variations over the export format). */
public interface Exporter {

    /** SD12 step 6. */
    List<String> format(List<Lead> leads);

    /** SD12 step 11. Disk write denied -> ValidationException("Cannot write there - choose another folder"). */
    void write(List<String> rows, String path);
}
