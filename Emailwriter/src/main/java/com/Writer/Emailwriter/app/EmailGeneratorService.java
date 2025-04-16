package com.Writer.Emailwriter.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public String generateEmailReply(EmailRequest emailRequest) {
        String prompt = buildPrompt(emailRequest);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        try {
            String fullUrl = geminiApiUrl + "?key=" + geminiApiKey;

            String response = webClient.post()
                    .uri(fullUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        System.err.println("Gemini API Error: " + errorBody);
                                        return Mono.error(new RuntimeException("Gemini API Error: " + errorBody));
                                    }))
                    .bodyToMono(String.class)
                    .block();

            return extractResponseContent(response);
        } catch (Exception e) {
            return "Error generating email: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        return "Generate a professional email reply for the following email content. "
                + "Please don't include a subject line. "
                + (emailRequest.getTone() != null ? "Use a " + emailRequest.getTone() + " tone.\n" : "")
                + "Original email:\n" + emailRequest.getEmailContent();
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            return "Error parsing Gemini response: " + e.getMessage();
        }
    }
}
