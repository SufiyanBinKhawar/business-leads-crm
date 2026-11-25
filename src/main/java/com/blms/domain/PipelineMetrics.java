package com.blms.domain;

import java.util.List;

/** D5 Figure 1 PipelineMetrics - Information Expert for its own stage counts (SD08 step 21). */
public class PipelineMetrics {

    private int totalLeads;
    private int discoveredCount;
    private int qualifiedCount;
    private int contactedCount;
    private int closedWonCount;
    private int closedLostCount;

    /** SD08 step 21. Archived leads are not counted. */
    public void recalculate(List<Lead> leads) {
        totalLeads = 0;
        discoveredCount = 0;
        qualifiedCount = 0;
        contactedCount = 0;
        closedWonCount = 0;
        closedLostCount = 0;
        for (Lead lead : leads) {
            if (lead.isArchived()) {
                continue;
            }
            totalLeads++;
            switch (lead.getStatus()) {
                case DISCOVERED -> discoveredCount++;
                case QUALIFIED -> qualifiedCount++;
                case CONTACTED -> contactedCount++;
                case CLOSED_WON -> closedWonCount++;
                case CLOSED_LOST -> closedLostCount++;
            }
        }
    }

    public int getTotalLeads() { return totalLeads; }
    public int getDiscoveredCount() { return discoveredCount; }
    public int getQualifiedCount() { return qualifiedCount; }
    public int getContactedCount() { return contactedCount; }
    public int getClosedWonCount() { return closedWonCount; }
    public int getClosedLostCount() { return closedLostCount; }
}
