package com.blms.service;

import com.blms.domain.Contact;
import com.blms.domain.EmailDraft;
import com.blms.domain.Lead;
import com.blms.domain.Workspace;
import com.blms.exception.IntegrationException;
import com.blms.integration.llm.LLMClient;

import java.util.ArrayList;
import java.util.List;

/**
 * D5 Figure 3 «Pure Fabrication» DraftGenerationService (+llm LLMClient). Prompt construction lives here, not in
 * Lead (High Cohesion). UC09 extension: an LLM failure skips that lead and the batch continues.
 */
public class DraftGenerationService {

    private final LLMClient llm;
    private int lastFailures;

    public DraftGenerationService(LLMClient llm) {
        this.llm = llm;
    }

    /** SD09 steps 8-15. */
    public List<EmailDraft> generate(List<Lead> leads, Workspace ws) {
        List<EmailDraft> drafts = new ArrayList<>();
        lastFailures = 0;
        for (Lead lead : leads) {
            try {
                String text = llm.complete(buildPrompt(lead, ws));
                int split = text.indexOf("\n\n");
                String subject = split > 0 ? text.substring(0, split).trim() : "Introduction";
                String body = split > 0 ? text.substring(split + 2).trim() : text.trim();
                drafts.add(lead.createDraft(subject, body));
            } catch (IntegrationException e) {
                lastFailures++;
            }
        }
        return drafts;
    }

    /** SD09 step 9: injects the {{variables}} (D3 UC09 step 3). */
    private String buildPrompt(Lead lead, Workspace ws) {
        Contact c = lead.getPrimaryContact();
        return """
                Write a short, professional B2B cold email.
                Sender company: {{company_name}}. Services: {{services}}.
                Recipient: {{contact_name}} ({{job_role}}) at {{lead_company}}, industry {{industry}}.
                Return the subject on the first line, then a blank line, then the body.
                """
                .replace("{{company_name}}", ws.getCompanyName())
                .replace("{{services}}", String.valueOf(ws.getServicesOffered()))
                .replace("{{contact_name}}", c.getFullName())
                .replace("{{job_role}}", String.valueOf(c.getJobRole()))
                .replace("{{lead_company}}", lead.getCompanyName())
                .replace("{{industry}}", String.valueOf(lead.getIndustry()));
    }

    /** NOT IN D5 - approved addition (gap G1): leads skipped in the last batch (UC09 extension report). */
    public int getLastFailures() {
        return lastFailures;
    }
}
