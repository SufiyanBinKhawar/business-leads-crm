package com.blms.integration.llm;

/**
 * D5 Figure 3 OpenAiLlmClient realizes LLMClient (UC09). REST POST with a JSON prompt payload.
 * Offline demo mode: blank apiKey returns a template email (CONFLICTS.md gap G12).
 */
public class OpenAiLlmClient implements LLMClient {

    private final String apiKey;

    public OpenAiLlmClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /** SD09 steps 10-11. Returns "subject line\n\nbody". Failure -> IntegrationException (UC09 extension). */
    @Override
    public String complete(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Quick idea for your team\n\nHi,\n\nI came across your company and believe our services "
                    + "could help you.\n\nWould you be open to a 15-minute call next week?\n\nBest regards";
        }
        // TODO(Sufiyan, UC09): POST https://api.openai.com/v1/chat/completions with Gson JSON body,
        //  header Authorization: Bearer apiKey; return choices[0].message.content
        throw new UnsupportedOperationException("TODO UC09 OpenAiLlmClient.complete");
    }
}
