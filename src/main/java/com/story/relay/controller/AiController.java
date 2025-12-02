package com.story.relay.controller;

import com.story.relay.dto.CharacterIndexRequestDto;
import com.story.relay.dto.ChatMessageRequestDto;
import com.story.relay.dto.ChatMessageResponseDto;
import com.story.relay.dto.ImageGenerationRequestDto;
import com.story.relay.dto.ImageGenerationResponseDto;
import com.story.relay.service.AnalysisAiClient;
import com.story.relay.service.ImageGenerationAiClient;
import com.story.relay.service.RagAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AiController {

    private final AnalysisAiClient analysisAiClient;
    private final ImageGenerationAiClient imageGenerationAiClient;
    private final RagAiClient ragAiClient;

    /**
     * Analyze novel text to extract summary, characters, and gauges
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeNovel(@RequestBody Map<String, Object> request) {
        log.info("=== Analyze Novel Request ===");
        Object novelText = request.get("novelText");
        if (novelText != null) {
            log.info("Novel text length: {} characters", novelText.toString().length());
        }

        Map<String, Object> response = analysisAiClient.analyze(request);
        log.info("Analysis completed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Generate full story via AI server
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateStory(@RequestBody Map<String, Object> request) {
        log.info("=== Generate Story Request ===");
        log.info("Request: {}", request.keySet());

        Map<String, Object> response = analysisAiClient.generate(request);
        log.info("Story generation completed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Generate image for a story node
     */
    @PostMapping("/generate-image")
    public ResponseEntity<ImageGenerationResponseDto> generateImage(
            @RequestBody ImageGenerationRequestDto request) {
        log.info("=== Generate Image Request ===");
        log.info("Node text: {}", request.getNodeText());
        log.info("Episode: {}", request.getEpisodeTitle());

        ImageGenerationResponseDto response = imageGenerationAiClient.generateImage(request);
        log.info("Image generation completed: {}", response.getImageUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * Index a character for RAG-based chat
     */
    @PostMapping("/chat/index-character")
    public ResponseEntity<Boolean> indexCharacter(@RequestBody CharacterIndexRequestDto request) {
        log.info("=== Index Character Request ===");
        log.info("Character: {} ({})", request.getName(), request.getCharacterId());

        boolean success = ragAiClient.indexCharacter(request);
        log.info("Character indexing {}", success ? "successful" : "failed");
        return ResponseEntity.ok(success);
    }

    /**
     * Send a message to character chatbot
     */
    @PostMapping("/chat/message")
    public ResponseEntity<ChatMessageResponseDto> sendChatMessage(
            @RequestBody ChatMessageRequestDto request) {
        log.info("=== Chat Message Request ===");
        log.info("Character: {}", request.getCharacterId());
        log.info("User message: {}", request.getUserMessage());

        ChatMessageResponseDto response = ragAiClient.sendMessage(request);
        log.info("Chat response: {}", response.getAiMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * Health check for relay server and AI servers
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Health check request");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("relayServer", "up");

        Map<String, Object> aiServers = new HashMap<>();

        // Check analysis AI
        Map<String, Object> analysisAiHealth = new HashMap<>();
        boolean analysisHealthy = analysisAiClient.checkHealth();
        analysisAiHealth.put("status", analysisHealthy ? "up" : "down");
        aiServers.put("analysisAi", analysisAiHealth);

        // Check image generation AI
        Map<String, Object> imageAiHealth = new HashMap<>();
        boolean imageHealthy = imageGenerationAiClient.checkHealth();
        imageAiHealth.put("status", imageHealthy ? "up" : "down");
        aiServers.put("imageGenerationAi", imageAiHealth);

        // Check RAG server
        Map<String, Object> ragAiHealth = new HashMap<>();
        boolean ragHealthy = ragAiClient.checkHealth();
        ragAiHealth.put("status", ragHealthy ? "up" : "down");
        aiServers.put("ragAi", ragAiHealth);

        health.put("aiServers", aiServers);

        return ResponseEntity.ok(health);
    }
}
