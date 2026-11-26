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
        String keywords = criteria == null ? "" : criteria.replaceFirst("^services=", "");
        return "industry=" + URLEncoder.encode(targetIndustry == null ? "" : targetIndustry, StandardCharsets.UTF_8)
                + "&keywords=" + URLEncoder.encode(keywords, StandardCharsets.UTF_8);
    }

    public Long getIcpId() { return icpId; }
    public void setIcpId(Long icpId) { this.icpId = icpId; }
    public String getTargetIndustry() { return targetIndustry; }
    public String getCriteria() { return criteria; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
