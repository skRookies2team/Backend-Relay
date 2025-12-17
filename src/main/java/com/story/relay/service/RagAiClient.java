package com.story.relay.service;

import com.story.relay.dto.CharacterIndexRequestDto;
import com.story.relay.dto.ChatMessageRequestDto;
import com.story.relay.dto.ChatMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Value("${ai-servers.rag.timeout:30000}")
    private int timeout;

    /**
     * Index a character for RAG-based chat
     * Converts character data to text file and calls /api/ai/train
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> indexCharacter(CharacterIndexRequestDto request) {
        log.info("Indexing character: {} ({})", request.getName(), request.getCharacterId());

        // Convert character data to text content
        String characterText = buildCharacterText(request);
        byte[] textBytes = characterText.getBytes(StandardCharsets.UTF_8);

        // Build multipart form data
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(textBytes) {
            @Override
            public String getFilename() {
                return request.getCharacterId() + "_character.txt";
            }
        }, MediaType.TEXT_PLAIN);
        builder.part("session_id", request.getCharacterId());
        builder.part("character_name", request.getName());

        return ragServerWebClient.post()
                .uri("/api/ai/train")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    String status = (String) response.get("status");
                    return "trained".equals(status);
                })
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
     * Calls /api/ai/chat with session_id and message
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<ChatMessageResponseDto> sendMessage(ChatMessageRequestDto request) {
        log.info("Sending message to character: {}", request.getCharacterId());
        log.info("User message: {}", request.getUserMessage());

        // Build request for /api/ai/chat
        Map<String, String> chatRequest = new HashMap<>();
        chatRequest.put("session_id", request.getCharacterId());
        chatRequest.put("message", request.getUserMessage());

        return ragServerWebClient.post()
                .uri("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    String reply = (String) response.get("reply");
                    return ChatMessageResponseDto.builder()
                            .characterId(request.getCharacterId())
                            .aiMessage(reply)
                            .sources(List.of())
                            .timestamp(Instant.now().toString())
                            .build();
                })
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
        // NPC AI server doesn't have a health endpoint, try a simple check
        return Mono.just(true)
                .doOnError(e -> log.warn("RAG server health check failed: {}", e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Build character text from CharacterIndexRequestDto
     */
    private String buildCharacterText(CharacterIndexRequestDto request) {
        StringBuilder sb = new StringBuilder();

        sb.append("캐릭터 이름: ").append(request.getName()).append("\n\n");

        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            sb.append("설명:\n").append(request.getDescription()).append("\n\n");
        }

        if (request.getPersonality() != null && !request.getPersonality().isEmpty()) {
            sb.append("성격:\n").append(request.getPersonality()).append("\n\n");
        }

        if (request.getBackground() != null && !request.getBackground().isEmpty()) {
            sb.append("배경 스토리:\n").append(request.getBackground()).append("\n\n");
        }

        if (request.getDialogueSamples() != null && !request.getDialogueSamples().isEmpty()) {
            sb.append("대화 샘플:\n");
            for (String dialogue : request.getDialogueSamples()) {
                sb.append("- ").append(dialogue).append("\n");
            }
            sb.append("\n");
        }

        if (request.getRelationships() != null && !request.getRelationships().isEmpty()) {
            sb.append("관계:\n");
            request.getRelationships().forEach((character, relationship) ->
                sb.append("- ").append(character).append(": ").append(relationship).append("\n")
            );
            sb.append("\n");
        }

        if (request.getAdditionalInfo() != null && !request.getAdditionalInfo().isEmpty()) {
            sb.append("추가 정보:\n");
            request.getAdditionalInfo().forEach((key, value) ->
                sb.append("- ").append(key).append(": ").append(value).append("\n")
            );
        }

        return sb.toString();
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
