package com.blms.integration.llm;

/** D5 Figure 3 «interface» LLMClient (Indirection, Protected Variations: hides the LLM vendor). */
public interface LLMClient {
    String complete(String prompt);
}
