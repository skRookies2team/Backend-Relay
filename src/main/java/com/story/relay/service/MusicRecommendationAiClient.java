package com.story.relay.service;

import com.story.relay.dto.MusicRequestDto;
import com.story.relay.dto.MusicResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicRecommendationAiClient {

    private final WebClient musicAiWebClient;

    /**
     * Recommend music based on scene description
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<MusicResponseDto> recommendMusic(MusicRequestDto request) {
        log.info("Requesting music recommendation for prompt: {}",
            request.getPrompt().length() > 50 ? request.getPrompt().substring(0, 50) + "..." : request.getPrompt());

        // Build request for AI-BGM server
        Map<String, String> bgmRequest = new HashMap<>();
        bgmRequest.put("prompt", request.getPrompt());

        return musicAiWebClient.post()
                .uri("/api/analyze")
                .bodyValue(bgmRequest)
                .retrieve()
                .bodyToMono(MusicResponseDto.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> {
                    if (response != null && response.getMusic() != null) {
                        log.info("Music recommended successfully: mood={}, file={}",
                            response.getMusic().getMood(),
                            response.getMusic().getFilename());
                    }
                })
                .doOnError(e -> log.error("Failed to recommend music: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.warn("Music recommendation failed, returning default response: {}", e.getMessage());
                    return Mono.just(createDefaultResponse());
                });
    }

    /**
     * Check if music AI server is healthy
     */
    public Mono<Boolean> checkHealth() {
        log.debug("Music AI health check");

        return musicAiWebClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> {
                    String status = (String) response.get("status");
                    return "healthy".equals(status);
                })
                .doOnError(e -> log.warn("Music AI health check failed: {}", e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Generate default response when AI-BGM server is unavailable
     */
    private MusicResponseDto createDefaultResponse() {
        return MusicResponseDto.builder()
                .analysis(MusicResponseDto.AnalysisData.builder()
                        .primary_mood("peaceful")
                        .intensity(0.5)
                        .reasoning("AI-BGM server unavailable, using default mood")
                        .build())
                .music(MusicResponseDto.MusicData.builder()
                        .mood("peaceful")
                        .filename("default.mp3")
                        .streaming_url(null)
                        .build())
                .build();
    }
}
