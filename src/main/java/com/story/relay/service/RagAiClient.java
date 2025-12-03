package com.story.relay.service;

import com.story.relay.dto.CharacterIndexRequestDto;
import com.story.relay.dto.ChatMessageRequestDto;
import com.story.relay.dto.ChatMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Client for RAG Server (Character Chat)
 * Communicates with Python RAG server for character-based chatbot
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagAiClient {

    private final WebClient ragServerWebClient;

    @Value("${ai-servers.rag.url:http://localhost:8002}")
    private String ragServerUrl;

    @Value("${ai-servers.rag.timeout:10000}")
    private int timeout;

    /**
     * Index a character for RAG-based chat
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> indexCharacter(CharacterIndexRequestDto request) {
        log.info("Indexing character: {} ({})", request.getName(), request.getCharacterId());

        return ragServerWebClient.post()
                .uri("/chat/index-character")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Boolean.class)
                .timeout(Duration.ofMillis(timeout))
                .map(success -> success != null && success)
                .doOnSuccess(success -> {
                    if (success) {
                        log.info("Character indexed successfully: {}", request.getCharacterId());
                    }
                })
                .doOnError(e -> log.error("Failed to index character {}: {}",
                        request.getCharacterId(), e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Send a message to character chatbot
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<ChatMessageResponseDto> sendMessage(ChatMessageRequestDto request) {
        log.info("Sending message to character: {}", request.getCharacterId());
        log.info("User message: {}", request.getUserMessage());

        return ragServerWebClient.post()
                .uri("/chat/message")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatMessageResponseDto.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnSuccess(response -> log.info("Received AI response: {}", response.getAiMessage()))
                .doOnError(e -> log.error("RAG server error: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Failed to get chat response, using fallback: {}", e.getMessage());
                    return Mono.just(generateFallbackResponse(request));
                })
                .switchIfEmpty(Mono.just(generateFallbackResponse(request)));
    }

    /**
     * Check RAG server health
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> checkHealth() {
        return ragServerWebClient.get()
                .uri("/chat/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> response != null)
                .doOnError(e -> log.warn("RAG server health check failed: {}", e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Generate fallback response when RAG server is unavailable
     */
    private ChatMessageResponseDto generateFallbackResponse(ChatMessageRequestDto request) {
        log.warn("Using fallback response for character: {}", request.getCharacterId());

        return ChatMessageResponseDto.builder()
            .characterId(request.getCharacterId())
            .aiMessage("죄송합니다. 지금은 대화가 어렵습니다. 잠시 후 다시 시도해주세요.")
            .sources(List.of())
            .timestamp(Instant.now().toString())
            .build();
    }
}
