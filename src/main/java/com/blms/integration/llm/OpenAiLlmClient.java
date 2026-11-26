package com.blms.integration.llm;

import com.blms.exception.IntegrationException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * D5 Figure 3 OpenAiLlmClient realizes LLMClient (UC09). REST POST with a JSON prompt payload (D3 UC09 technology
 * variation). GoF Adapter: turns the OpenAI Chat Completions API into LLMClient.complete(prompt).
 * Offline demo mode: blank apiKey returns a template email (CONFLICTS.md gap G12).
 */
public class OpenAiLlmClient implements LLMClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final Duration TIMEOUT = Duration.ofSeconds(45);

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
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        JsonArray messages = new JsonArray();
        messages.add(message);
        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        HttpResponse<String> response;
        try {
            response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new IntegrationException("LLM API failure: the request timed out", e);
        } catch (IOException e) {
            throw new IntegrationException("LLM API failure: cannot reach the LLM service", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("LLM API failure: generation was cancelled", e);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IntegrationException("LLM API failure (HTTP " + response.statusCode() + ")");
        }
        return contentOf(response.body());
    }

    /** choices[0].message.content, null-safe. */
    private static String contentOf(String body) {
        try {
            JsonElement root = JsonParser.parseString(body == null ? "" : body);
            if (root.isJsonObject()) {
                JsonElement choices = root.getAsJsonObject().get("choices");
                if (choices != null && choices.isJsonArray() && !choices.getAsJsonArray().isEmpty()
                        && choices.getAsJsonArray().get(0).isJsonObject()) {
                    JsonElement msg = choices.getAsJsonArray().get(0).getAsJsonObject().get("message");
                    if (msg != null && msg.isJsonObject()) {
                        JsonElement content = msg.getAsJsonObject().get("content");
                        if (content != null && content.isJsonPrimitive() && !content.getAsString().isBlank()) {
                            return content.getAsString().trim();
                        }
                    }
                }
            }
        } catch (JsonParseException | IllegalStateException e) {
            throw new IntegrationException("LLM API failure: unexpected response", e);
        }
        throw new IntegrationException("LLM API failure: the response contained no text");
    }
}
