package org.superjoin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private String API_KEY = "src/main/resources/gemini.key";

    public GeminiService() {
        try {
            API_KEY = Files.readString(Paths.get(API_KEY)).trim();
        } catch (Exception e) {
            System.err.println("Failed to read API key: " + e.getMessage());
        }
    }

    public String convertToCypher(String prompt) {
        String response = callGemini("Convert this natural language query to Cypher: " + prompt);
        return extractCode(response);
    }

    public String getSuggestions(String graphSummary) {
        String response = callGemini("Suggest improvements for this graph: " + graphSummary);
        return response;
    }

    private String callGemini(String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt))))
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL + "?key=" + API_KEY, entity, String.class);
        System.out.println(response);
        return response.getBody();
    }

    private String extractCode(String response) {
        try {
            System.out.println(response);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray()) {
                for (JsonNode candidate : candidates) {
                    JsonNode parts = candidate.path("content").path("parts");
                    for (JsonNode part : parts) {
                        String text = part.path("text").asText("");
                        // Look for cypher code block
                        if (text.contains("```cypher")) {
                            int start = text.indexOf("```cypher") + 9;
                            int end = text.indexOf("```", start);
                            if (end > start) {
                                return text.substring(start, end).trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing Gemini response: " + e.getMessage());
        }

        return null;
    }
}