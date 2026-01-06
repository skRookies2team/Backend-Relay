package com.story.relay.service;

import com.story.relay.dto.CharacterIndexRequestDto;
import com.story.relay.dto.CharacterSetRequestDto;
import com.story.relay.dto.ChatMessageRequestDto;
import com.story.relay.dto.ChatMessageResponseDto;
import com.story.relay.dto.GameProgressUpdateRequestDto;
import com.story.relay.dto.NovelIndexRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
     * Uses /api/ai/character endpoint to set character information
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> indexCharacter(CharacterIndexRequestDto request) {
        log.info("Indexing character: {} ({})", request.getName(), request.getCharacterId());

        // Build character description from request data
        String characterDescription = buildCharacterDescription(request);

        // Build request for /api/ai/character
        Map<String, String> characterRequest = new HashMap<>();
        characterRequest.put("session_id", request.getCharacterId());
        characterRequest.put("character_name", request.getName());
        characterRequest.put("character_description", characterDescription);

        return ragServerWebClient.post()
                .uri("/api/ai/character")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(characterRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    String status = (String) response.get("status");
                    return "character_set".equals(status);
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
     * Index a novel for RAG-based character chat
     * Calls AI-NPC's /api/ai/train-from-s3 endpoint
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> indexNovel(NovelIndexRequestDto request) {
        log.info("Indexing novel: {} ({})", request.getTitle(), request.getStoryId());
        log.info("File key: {}, Bucket: {}", request.getFileKey(), request.getBucket());

        // Build request for /api/ai/train-from-s3
        Map<String, String> trainRequest = new HashMap<>();
        trainRequest.put("session_id", request.getStoryId());
        trainRequest.put("file_key", request.getFileKey());
        trainRequest.put("bucket", request.getBucket());
        // character_name은 보내지 않음 - 나중에 setCharacter에서 실제 캐릭터들을 설정
        trainRequest.put("character_name", "");  // 빈 문자열로 전송하여 기본값 사용

        return ragServerWebClient.post()
                .uri("/api/ai/train-from-s3")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trainRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    String status = (String) response.get("status");
                    return "trained".equals(status);
                })
                .doOnSuccess(success -> {
                    if (success) {
                        log.info("Novel indexed successfully: {}", request.getStoryId());
                    }
                })
                .doOnError(e -> log.error("Failed to index novel {}: {}",
                        request.getStoryId(), e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Send a message to character chatbot
     * Calls /api/ai/chat with session_id and message
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<ChatMessageResponseDto> sendMessage(ChatMessageRequestDto request) {
        log.info("Sending message to character: {} ({})", request.getCharacterName(), request.getCharacterId());
        log.info("Story ID: {}", request.getStoryId());
        log.info("User message: {}", request.getUserMessage());

        // Build request for /api/ai/chat
        // session_id는 storyId로 설정 (벡터 스토어 매칭용)
        // character_name은 캐릭터 페르소나 설정용
        Map<String, String> chatRequest = new HashMap<>();

        // storyId를 session_id로 사용 (Python AI 서버의 벡터 스토어 검색 키)
        String sessionId = (request.getStoryId() != null && !request.getStoryId().isEmpty())
                ? request.getStoryId()
                : request.getCharacterId();  // fallback: storyId가 없으면 characterId 사용

        // characterName 처리: null이면 characterId에서 추출 시도
        String characterName = request.getCharacterName();
        if (characterName == null || characterName.isEmpty()) {
            // characterId에서 캐릭터 이름 추출 (story_39a5d3b1_로미오 → 로미오)
            String characterId = request.getCharacterId();
            if (characterId != null && characterId.startsWith("story_") && characterId.contains("_")) {
                String[] parts = characterId.split("_");
                if (parts.length >= 3) {
                    characterName = String.join("_", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                    log.info("CharacterId에서 캐릭터 이름 추출: {} → {}", characterId, characterName);
                } else {
                    characterName = "캐릭터"; // 기본값
                }
            } else {
                characterName = "캐릭터"; // 기본값
            }
        }

        chatRequest.put("session_id", sessionId);
        chatRequest.put("character_name", characterName);
        chatRequest.put("message", request.getUserMessage());

        log.info("Sending to Python AI server - session_id: {}, character_name: {}", sessionId, chatRequest.get("character_name"));

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
        log.debug("Checking RAG server health");

        return ragServerWebClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> {
                    // Check if response has a valid status field
                    Object status = response.get("status");
                    return status != null && "running".equals(status.toString());
                })
                .doOnSuccess(healthy -> {
                    if (healthy) {
                        log.debug("RAG server is healthy");
                    } else {
                        log.warn("RAG server responded but status is not 'running'");
                    }
                })
                .doOnError(e -> log.warn("RAG server health check failed: {}", e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Build character description from CharacterIndexRequestDto
     */
    private String buildCharacterDescription(CharacterIndexRequestDto request) {
        StringBuilder sb = new StringBuilder();

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

    /**
     * Update game progress to NPC AI server
     * Called when player makes choices and progresses through story
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> updateGameProgress(GameProgressUpdateRequestDto request) {
        log.info("Updating game progress for character: {}", request.getCharacterId());

        // Build request for /api/ai/update
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("session_id", request.getCharacterId());
        updateRequest.put("content", request.getContent());
        updateRequest.put("metadata", request.getMetadata() != null ? request.getMetadata() : new HashMap<>());

        return ragServerWebClient.post()
                .uri("/api/ai/update")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    String status = (String) response.get("status");
                    return "updated".equals(status);
                })
                .doOnSuccess(success -> {
                    if (success) {
                        log.info("Game progress updated successfully for: {}", request.getCharacterId());
                    }
                })
                .doOnError(e -> log.error("Failed to update game progress for {}: {}",
                        request.getCharacterId(), e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Set character information without training
     * Updates character persona/description in the RAG system
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> setCharacter(CharacterSetRequestDto request) {
        log.info("Setting character: {} ({})", request.getCharacterName(), request.getCharacterId());

        // Build request for /api/ai/character
        Map<String, String> characterRequest = new HashMap<>();
        characterRequest.put("session_id", request.getCharacterId());
        characterRequest.put("character_name", request.getCharacterName());
        if (request.getCharacterDescription() != null && !request.getCharacterDescription().isEmpty()) {
            characterRequest.put("character_description", request.getCharacterDescription());
        } else {
            characterRequest.put("character_description", "");
        }

        return ragServerWebClient.post()
                .uri("/api/ai/character")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(characterRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    String status = (String) response.get("status");
                    return "character_set".equals(status);
                })
                .doOnSuccess(success -> {
                    if (success) {
                        log.info("Character set successfully: {}", request.getCharacterId());
                    }
                })
                .doOnError(e -> log.error("Failed to set character {}: {}",
                        request.getCharacterId(), e.getMessage()))
                .onErrorReturn(false);
    }

}
