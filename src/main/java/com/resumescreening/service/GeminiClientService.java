package com.resumescreening.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.exception.LlmProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Thin client around the Google Gemini "generateContent" REST API.
 *
 * Endpoint reference (v1beta):
 *   POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 *   Header: x-goog-api-key: <API_KEY>
 *
 * The model name and API key are externalised in application.properties so they
 * can be swapped without touching code (e.g. gemini-2.5-flash, gemini-2.0-flash, etc.)
 */
@Service
public class GeminiClientService {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    @Value("${https://aistudio.google.com/api-keys?project=gen-lang-client-0200989972}")
    private String baseUrl;

    @Value("${gemini.api.temperature:0.2}")
    private double temperature;

    public GeminiClientService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Sends the prompt to Gemini and returns the raw text response
     * (expected to be a JSON string, per our prompt's output-format instructions).
     */
    public String generateContent(String prompt) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("AQ.Ab8RN6KgMF7WgPWrTVTtTcqbXMCp7A9zRgTMi2giT9FcdK7edA")) {
            throw new LlmProcessingException(
                "Gemini API key is not configured. Set GEMINI_API_KEY environment variable " +
                "or gemini.api.key in application.properties.");
        }

        String url = baseUrl + "/" + model + ":generateContent";

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "temperature", temperature,
                "responseMimeType", "application/json"
            )
        );

        try {
            String response = webClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractText(response);
        } catch (Exception ex) {
            log.error("Gemini API call failed", ex);
            throw new LlmProcessingException("Failed to call Gemini API: " + ex.getMessage(), ex);
        }
    }

    /** Extracts candidates[0].content.parts[0].text from the Gemini response envelope */
    private String extractText(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new LlmProcessingException("Gemini returned no candidates. Raw response: " + rawJson);
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new LlmProcessingException("Gemini response missing content parts. Raw response: " + rawJson);
            }
            return parts.get(0).path("text").asText();
        } catch (LlmProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmProcessingException("Could not parse Gemini response: " + rawJson, e);
        }
    }
}
