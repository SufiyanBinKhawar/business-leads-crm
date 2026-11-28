package com.blms.domain;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/** D5 Figure 1 IdealCustomerProfile - composed by Workspace (defines ICP, +icp 0..1). */
public class IdealCustomerProfile {

    private Long icpId;
    private final String targetIndustry;
    private final String criteria;
    private final LocalDateTime generatedAt;

    /** SD02 step 11: «create» IdealCustomerProfile(targetIndustry, services). */
    public IdealCustomerProfile(String targetIndustry, String services) {
        this(null, targetIndustry, "services=" + services, LocalDateTime.now());
    }

    /** Used by WorkspaceRepository when loading. */
    public IdealCustomerProfile(Long icpId, String targetIndustry, String criteria, LocalDateTime generatedAt) {
        this.icpId = icpId;
        this.targetIndustry = targetIndustry;
        this.criteria = criteria;
        this.generatedAt = generatedAt;
    }

    /** SD05 steps 11-12: the ICP builds its own search query (Information Expert). */
    public String toSearchQuery() {
        // Apollo People API Search parameters (CONFLICTS.md I-002). The target industry goes in q_keywords, a free-text
        // match. Apollo's organization_industries[] filter only accepts its own exact lowercase names, so a typed
        // industry such as "Fintech" would return nothing. per_page 10 = one enrichment call (<= 10 credits) per run.
        return "q_keywords=" + URLEncoder.encode(targetIndustry == null ? "" : targetIndustry.trim(), StandardCharsets.UTF_8)
                + "&per_page=10&page=1";
    }

    public Long getIcpId() { return icpId; }
    public void setIcpId(Long icpId) { this.icpId = icpId; }
    public String getTargetIndustry() { return targetIndustry; }
    public String getCriteria() { return criteria; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
