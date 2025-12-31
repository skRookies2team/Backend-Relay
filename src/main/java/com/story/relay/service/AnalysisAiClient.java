package com.story.relay.service;

import com.story.relay.dto.SubtreeRegenerationRequestDto;
import com.story.relay.dto.SubtreeRegenerationResponseDto;
import com.story.relay.exception.AiServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisAiClient {

    private final WebClient analysisAiWebClient;

    /**
     * Analyze novel text to extract summary, characters, and gauges
     * Returns a reactive Mono for non-blocking execution
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> analyze(Map<String, Object> request) {
        log.info("Calling analysis AI server for novel analysis");

        return analysisAiWebClient.post()
            .uri("/analyze")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (Map<String, Object>) response)
            .timeout(Duration.ofMinutes(3))
            .doOnSuccess(response -> log.info("Novel analysis completed successfully"))
            .doOnError(e -> log.error("AI server error during analysis: {}", e.getMessage(), e))
            .onErrorMap(e -> new AiServerException("ANALYSIS-AI", "Analysis failed: " + e.getMessage(), e))
            .switchIfEmpty(Mono.error(new AiServerException("ANALYSIS-AI", "No response from analysis AI server")));
    }

    /**
     * Analyze novel from S3
     * Returns a reactive Mono for non-blocking execution
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> analyzeFromS3(Map<String, Object> request) {
        log.info("Calling analysis AI server for S3 novel analysis");

        return analysisAiWebClient.post()
            .uri("/analyze-from-s3")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (Map<String, Object>) response)
            .timeout(Duration.ofMinutes(3))
            .doOnSuccess(response -> log.info("S3 novel analysis completed successfully"))
            .doOnError(e -> log.error("AI server error during S3 analysis: {}", e.getMessage(), e))
            .onErrorMap(e -> new AiServerException("ANALYSIS-AI", "S3 analysis failed: " + e.getMessage(), e))
            .switchIfEmpty(Mono.error(new AiServerException("ANALYSIS-AI", "No response from analysis AI server")));
    }

    /**
     * Generate full story via AI server
     * Returns a reactive Mono for non-blocking execution
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> generate(Map<String, Object> request) {
        log.info("Calling analysis AI server for story generation");

        return analysisAiWebClient.post()
            .uri("/generate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (Map<String, Object>) response)
            .timeout(Duration.ofMinutes(10))
            .doOnSuccess(response -> log.info("Story generation completed successfully"))
            .doOnError(e -> log.error("AI server error during generation: {}", e.getMessage(), e))
            .onErrorMap(e -> new AiServerException("ANALYSIS-AI", "Story generation failed: " + e.getMessage(), e))
            .switchIfEmpty(Mono.error(new AiServerException("ANALYSIS-AI", "No response from analysis AI server")));
    }

    /**
     * Generate next episode
     * Returns a reactive Mono for non-blocking execution
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> generateNextEpisode(Map<String, Object> request) {
        log.info("Calling analysis AI server for next episode generation");

        return analysisAiWebClient.post()
            .uri("/generate-next-episode")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (Map<String, Object>) response)
            .timeout(Duration.ofMinutes(10))
            .doOnSuccess(response -> log.info("Next episode generation completed successfully"))
            .doOnError(e -> log.error("AI server error during next episode generation: {}", e.getMessage(), e))
            .onErrorMap(e -> new AiServerException("ANALYSIS-AI", "Next episode generation failed: " + e.getMessage(), e))
            .switchIfEmpty(Mono.error(new AiServerException("ANALYSIS-AI", "No response from analysis AI server")));
    }

    /**
     * Finalize analysis - generate final endings based on selected gauges
     * Returns a reactive Mono for non-blocking execution
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> finalizeAnalysis(Map<String, Object> request) {
        log.info("Calling analysis AI server for finalizing analysis (generating final endings)");

        return analysisAiWebClient.post()
            .uri("/finalize-analysis")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (Map<String, Object>) response)
            .timeout(Duration.ofMinutes(3))
            .doOnSuccess(response -> log.info("Final endings generation completed successfully"))
            .doOnError(e -> log.error("AI server error during finalize analysis: {}", e.getMessage(), e))
            .onErrorMap(e -> new AiServerException("ANALYSIS-AI", "Finalize analysis failed: " + e.getMessage(), e))
            .switchIfEmpty(Mono.error(new AiServerException("ANALYSIS-AI", "No response from analysis AI server")));
    }

    /**
     * Regenerate subtree from a modified node
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<SubtreeRegenerationResponseDto> regenerateSubtree(SubtreeRegenerationRequestDto request) {
        log.info("Calling analysis AI server for subtree regeneration");
        log.info("Parent node: {}, current depth: {}, max depth: {}",
            request.getParentNode().getNodeId(), request.getCurrentDepth(), request.getMaxDepth());

        return analysisAiWebClient.post()
            .uri("/regenerate-subtree")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(SubtreeRegenerationResponseDto.class)
            .timeout(Duration.ofMinutes(5))
            .doOnSuccess(response -> log.info("Subtree regeneration completed: {} nodes regenerated",
                response.getTotalNodesRegenerated()))
            .doOnError(e -> log.error("AI server error during subtree regeneration: {}", e.getMessage(), e))
            .onErrorMap(e -> new AiServerException("ANALYSIS-AI", "Subtree regeneration failed: " + e.getMessage(), e))
            .switchIfEmpty(Mono.error(new AiServerException("ANALYSIS-AI", "No response from analysis AI server")));
    }

    /**
     * Check if analysis AI server is healthy
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> checkHealth() {
        return analysisAiWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> response != null)
                .doOnError(e -> log.warn("Analysis AI health check failed: {}", e.getMessage()))
                .onErrorReturn(false);
    }
}
