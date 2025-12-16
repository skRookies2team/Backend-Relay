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
        log.info("Received request keys: {}", request.keySet());

        // Check both camelCase and snake_case
        Object novelText = request.get("novelText");
        Object novel_text = request.get("novel_text");

        log.info("novelText (camelCase) is null: {}", novelText == null);
        log.info("novel_text (snake_case) is null: {}", novel_text == null);

        if (novelText != null) {
            log.info("novelText length: {} characters", novelText.toString().length());
        }
        if (novel_text != null) {
            log.info("novel_text length: {} characters", novel_text.toString().length());
        }

        return analysisAiClient.analyze(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Analysis completed successfully"));
    }

    /**
     * Analyze novel from S3
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "S3에서 소설 분석")
    @PostMapping("/analyze-from-s3")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeNovelFromS3(@RequestBody Map<String, Object> request) {
        log.info("=== Analyze Novel From S3 Request ===");
        log.info("Received request keys: {}", request.keySet());

        return analysisAiClient.analyzeFromS3(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("S3 analysis completed successfully"));
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
     * Finalize analysis - generate final endings based on selected gauges
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "분석 완료 - 선택된 게이지로 최종 엔딩 생성")
    @PostMapping("/finalize-analysis")
    public Mono<ResponseEntity<Map<String, Object>>> finalizeAnalysis(@RequestBody Map<String, Object> request) {
        log.info("=== Finalize Analysis Request ===");
        log.info("Request keys: {}", request.keySet());

        return analysisAiClient.finalizeAnalysis(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Finalize analysis completed successfully"));
    }

    /**
     * Generate next episode
     * Returns a reactive Mono for non-blocking execution
     */
    @Operation(summary = "다음 에피소드 생성")
    @PostMapping("/generate-next-episode")
    public Mono<ResponseEntity<Map<String, Object>>> generateNextEpisode(@RequestBody Map<String, Object> request) {
        log.info("=== Generate Next Episode Request ===");
        log.info("Request: {}", request.keySet());

        return analysisAiClient.generateNextEpisode(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Next episode generation completed successfully"));
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
