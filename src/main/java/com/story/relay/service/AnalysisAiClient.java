package com.story.relay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisAiClient {

    private final WebClient analysisAiWebClient;

    /**
     * Analyze novel text to extract summary, characters, and gauges
     */
    public Map<String, Object> analyze(Map<String, Object> request) {
        log.info("Calling analysis AI server for novel analysis");

        Map<String, Object> response = analysisAiWebClient.post()
            .uri("/analyze")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofMinutes(3))
            .onErrorResume(e -> {
                log.error("AI server error during analysis: {}", e.getMessage(), e);
                throw new RuntimeException("Analysis failed: " + e.getMessage());
            })
            .block();

        if (response == null) {
            throw new RuntimeException("No response from analysis AI server");
        }

        log.info("Novel analysis completed successfully");
        return response;
    }

    /**
     * Generate full story via AI server
     */
    public Map<String, Object> generate(Map<String, Object> request) {
        log.info("Calling analysis AI server for story generation");

        Map<String, Object> response = analysisAiWebClient.post()
            .uri("/generate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofMinutes(10))
            .onErrorResume(e -> {
                log.error("AI server error during generation: {}", e.getMessage(), e);
                throw new RuntimeException("Story generation failed: " + e.getMessage());
            })
            .block();

        if (response == null) {
            throw new RuntimeException("No response from analysis AI server");
        }

        log.info("Story generation completed successfully");
        return response;
    }

    /**
     * Check if analysis AI server is healthy
     */
    public boolean checkHealth() {
        try {
            String response = analysisAiWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            return response != null;
        } catch (Exception e) {
            log.warn("Analysis AI health check failed: {}", e.getMessage());
            return false;
        }
    }
}
