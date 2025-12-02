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
     */
    public boolean indexCharacter(CharacterIndexRequestDto request) {
        try {
            log.info("Indexing character: {} ({})", request.getName(), request.getCharacterId());

            Boolean success = ragServerWebClient.post()
                .uri("/chat/index-character")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Boolean.class)
                .timeout(Duration.ofMillis(timeout))
                .block();

            log.info("Character indexed successfully: {}", request.getCharacterId());
            return success != null && success;

        } catch (Exception e) {
            log.error("Failed to index character {}: {}", request.getCharacterId(), e.getMessage());
            return false;
        }
    }

    /**
     * Send a message to character chatbot
     */
    public ChatMessageResponseDto sendMessage(ChatMessageRequestDto request) {
        log.info("Sending message to character: {}", request.getCharacterId());
        log.info("User message: {}", request.getUserMessage());

        try {
            ChatMessageResponseDto response = ragServerWebClient.post()
                .uri("/chat/message")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatMessageResponseDto.class)
                .timeout(Duration.ofMillis(timeout))
                .onErrorResume(e -> {
                    log.error("RAG server error: {}", e.getMessage());
                    return Mono.just(generateFallbackResponse(request));
                })
                .block();

            if (response == null) {
                return generateFallbackResponse(request);
            }

            log.info("Received AI response: {}", response.getAiMessage());
            return response;

        } catch (Exception e) {
            log.error("Failed to get chat response: {}", e.getMessage());
            return generateFallbackResponse(request);
        }
    }

    /**
     * Check RAG server health
     */
    public boolean checkHealth() {
        try {
            String response = ragServerWebClient.get()
                .uri("/chat/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();

            return response != null;
        } catch (Exception e) {
            log.warn("RAG server health check failed: {}", e.getMessage());
            return false;
        }
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
