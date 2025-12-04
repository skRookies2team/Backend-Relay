package com.story.relay.controller;

import com.story.relay.dto.CharacterIndexRequestDto;
import com.story.relay.dto.ChatMessageRequestDto;
import com.story.relay.dto.ChatMessageResponseDto;
import com.story.relay.dto.ImageGenerationRequestDto;
import com.story.relay.dto.ImageGenerationResponseDto;
import com.story.relay.dto.SubtreeRegenerationRequestDto;
import com.story.relay.dto.SubtreeRegenerationResponseDto;
import com.story.relay.service.AnalysisAiClient;
import com.story.relay.service.ImageGenerationAiClient;
import com.story.relay.service.RagAiClient;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AnalysisAiClient analysisAiClient;
    private final ImageGenerationAiClient imageGenerationAiClient;
    private final RagAiClient ragAiClient;

    /**
     * Analyze novel text to extract summary, characters, and gauges
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "소설 분석")
    @PostMapping("/analyze")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeNovel(@RequestBody Map<String, Object> request) {
        log.info("=== Analyze Novel Request ===");
        Object novelText = request.get("novelText");
        if (novelText != null) {
            log.info("Novel text length: {} characters", novelText.toString().length());
        }

        return analysisAiClient.analyze(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Analysis completed successfully"));
    }

    /**
     * Generate full story via AI server
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "소설 생성")
    @PostMapping("/generate")
    public Mono<ResponseEntity<Map<String, Object>>> generateStory(@RequestBody Map<String, Object> request) {
        log.info("=== Generate Story Request ===");
        log.info("Request: {}", request.keySet());

        return analysisAiClient.generate(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Story generation completed successfully"));
    }

    /**
     * Generate image for a story node
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "이미지 생성")
    @PostMapping("/generate-image")
    public Mono<ResponseEntity<ImageGenerationResponseDto>> generateImage(
            @Valid @RequestBody ImageGenerationRequestDto request) {
        log.info("=== Generate Image Request ===");
        log.info("Node text: {}", request.getNodeText());
        log.info("Episode: {}", request.getEpisodeTitle());

        return imageGenerationAiClient.generateImage(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Image generation completed: {}",
                        response.getBody().getImageUrl()));
    }

    /**
     * Regenerate subtree from a modified node
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "서브트리 재생성")
    @PostMapping("/regenerate-subtree")
    public Mono<ResponseEntity<SubtreeRegenerationResponseDto>> regenerateSubtree(
            @Valid @RequestBody SubtreeRegenerationRequestDto request) {
        log.info("=== Regenerate Subtree Request ===");
        log.info("Episode: {} (order {})", request.getEpisodeTitle(), request.getEpisodeOrder());
        log.info("Parent node: {}, depth: {}/{}", request.getParentNode().getNodeId(),
            request.getCurrentDepth(), request.getMaxDepth());

        return analysisAiClient.regenerateSubtree(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Subtree regeneration completed: {} nodes",
                        response.getBody().getTotalNodesRegenerated()));
    }

    /**
     * Index a character for RAG-based chat
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "캐릭터 인덱싱")
    @PostMapping("/chat/index-character")
    public Mono<ResponseEntity<Boolean>> indexCharacter(@Valid @RequestBody CharacterIndexRequestDto request) {
        log.info("=== Index Character Request ===");
        log.info("Character: {} ({})", request.getName(), request.getCharacterId());

        return ragAiClient.indexCharacter(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Character indexing {}",
                        response.getBody() ? "successful" : "failed"));
    }

    /**
     * Send a message to character chatbot
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "캐릭터 챗봇 메시지 전송")
    @PostMapping("/chat/message")
    public Mono<ResponseEntity<ChatMessageResponseDto>> sendChatMessage(
            @Valid @RequestBody ChatMessageRequestDto request) {
        log.info("=== Chat Message Request ===");
        log.info("Character: {}", request.getCharacterId());
        log.info("User message: {}", request.getUserMessage());

        return ragAiClient.sendMessage(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Chat response: {}",
                        response.getBody().getAiMessage()));
    }

    /**
     * Health check for relay server and AI servers
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "헬스 체크")
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        log.debug("Health check request");

        return Mono.zip(
                analysisAiClient.checkHealth(),
                imageGenerationAiClient.checkHealth(),
                ragAiClient.checkHealth()
        ).map(tuple -> {
            boolean analysisHealthy = tuple.getT1();
            boolean imageHealthy = tuple.getT2();
            boolean ragHealthy = tuple.getT3();

            Map<String, Object> health = new HashMap<>();
            health.put("status", "healthy");
            health.put("relayServer", "up");

            Map<String, Object> aiServers = new HashMap<>();

            Map<String, Object> analysisAiHealth = new HashMap<>();
            analysisAiHealth.put("status", analysisHealthy ? "up" : "down");
            aiServers.put("analysisAi", analysisAiHealth);

            Map<String, Object> imageAiHealth = new HashMap<>();
            imageAiHealth.put("status", imageHealthy ? "up" : "down");
            aiServers.put("imageGenerationAi", imageAiHealth);

            Map<String, Object> ragAiHealth = new HashMap<>();
            ragAiHealth.put("status", ragHealthy ? "up" : "down");
            aiServers.put("ragAi", ragAiHealth);

            health.put("aiServers", aiServers);

            return ResponseEntity.ok(health);
        });
    }
}
