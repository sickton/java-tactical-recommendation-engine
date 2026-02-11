package com.sickton.jgaffer.openAIService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class OpenAIClient {

    private static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/responses";

    private final String apiKey;
    private final HttpClient httpClient;

    public OpenAIClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Sends a prompt to OpenAI and returns a natural language explanation.
     * This method NEVER influences decision logic — it only explains it.
     */
    public String explain(String prompt) throws Exception {

        // Escape quotes to keep JSON valid
        String escapedPrompt = escapeForJson(prompt);

        String requestBody = """
        {
          "model": "gpt-4.1",
          "input": "%s",
          "temperature": 0.3,
          "max_output_tokens": 180
        }
        """.formatted(escapedPrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API call failed");
        }

        return extractText(response.body());
    }

    private String extractText(String responseBody) {

        // Look for the first occurrence of "text":"..."
        int index = responseBody.indexOf("\"text\"");
        if (index == -1) {
            return "AI explanation received, but could not be parsed.";
        }

        int start = responseBody.indexOf('"', index + 6) + 1;
        int end = responseBody.indexOf('"', start);

        if (start < 0 || end < 0) {
            return "AI explanation received, but parsing failed.";
        }

        return responseBody.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    private String escapeForJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}