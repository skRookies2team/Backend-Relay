package com.story.relay.service;

import com.story.relay.dto.ImageGenerationRequestDto;
import com.story.relay.dto.ImageGenerationResponseDto;
import com.story.relay.dto.NovelStyleLearnRequestDto;
import com.story.relay.dto.NovelStyleLearnResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationAiClient {

    private final WebClient imageGenerationAiWebClient;
    private final S3UploadService s3UploadService;

    /**
     * Learn novel style in AI-IMAGE server
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<NovelStyleLearnResponseDto> learnNovelStyle(NovelStyleLearnRequestDto request) {
        log.info("Learning novel style for story: {}", request.getStory_id());

        return imageGenerationAiWebClient.post()
                .uri("/api/v1/learn-style")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NovelStyleLearnResponseDto.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> {
                    if (response != null && response.getThumbnail_image_url() != null) {
                        log.info("Novel style learned successfully with thumbnail: {}", response.getThumbnail_image_url());
                    } else {
                        log.info("Novel style learned successfully for story: {}", request.getStory_id());
                    }
                })
                .doOnError(e -> {
                    log.error("Failed to learn novel style: {}", e.getMessage(), e);
                    if (e instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) e;
                        log.error("AI-IMAGE server error - Status: {}, Response: {}",
                            wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    }
                })
                .onErrorReturn(NovelStyleLearnResponseDto.builder().build());
    }

    /**
     * Generate image via AI-IMAGE server
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<ImageGenerationResponseDto> generateImage(ImageGenerationRequestDto request) {
        if (request.getGenerateImage() != null && !request.getGenerateImage()) {
            log.info("Image generation skipped as per request.");
            return Mono.just(ImageGenerationResponseDto.builder().build());
        }

        log.info("=== Image Generation Request from Backend ===");
        log.info("  📍 Story ID: {}", request.getStoryId());
        log.info("  📍 Node ID: {}", request.getNodeId());
        log.info("  📍 Episode: {} (order: {})", request.getEpisodeTitle(), request.getEpisodeOrder());
        log.info("  📍 Image Type: {}", request.getImageType());
        log.info("  🔗 Image S3 URL: {}", request.getImageS3Url() != null ?
            (request.getImageS3Url().length() > 100 ? request.getImageS3Url().substring(0, 100) + "..." : request.getImageS3Url())
            : "NULL");
        log.debug("Request details: nodeText={}, situation={}, episodeTitle={}",
            request.getNodeText(), request.getSituation(), request.getEpisodeTitle());

        // Build prompt from node information
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(request.getEpisodeTitle()).append(": ");
        promptBuilder.append(request.getNodeText());

        if (request.getSituation() != null && !request.getSituation().isEmpty()) {
            promptBuilder.append(". ").append(request.getSituation());
        }

        // Build request for AI-IMAGE server
        String storyId = request.getStoryId() != null ? request.getStoryId() : "story_" + UUID.randomUUID().toString();

        Map<String, Object> aiImageRequest = new HashMap<>();
        aiImageRequest.put("story_id", storyId);
        aiImageRequest.put("user_prompt", promptBuilder.toString());
        aiImageRequest.put("context_text", request.getNodeText());  // AI-IMAGE 서버는 context_text 사용

        // S3 presigned URL 전달 (백엔드에서 생성한 업로드용 URL)
        if (request.getImageS3Url() != null && !request.getImageS3Url().isEmpty()) {
            aiImageRequest.put("s3_url", request.getImageS3Url());
            log.info("  ✅ S3 presigned URL included for AI-IMAGE server");
            log.debug("     URL: {}", request.getImageS3Url().substring(0, Math.min(150, request.getImageS3Url().length())));
        } else {
            log.error("  ❌ No S3 presigned URL provided - AI-IMAGE server will fail!");
            log.error("     imageS3Url is: {}", request.getImageS3Url() == null ? "NULL" : "EMPTY STRING");
        }

        return imageGenerationAiWebClient.post()
                .uri("/api/v1/generate-image")
                .bodyValue(aiImageRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .doOnError(e -> log.error("AI-IMAGE server error during image generation: {}", e.getMessage(), e))
                .onErrorResume(e -> {
                    log.warn("Image generation failed, returning mock image: {}", e.getMessage());
                    return Mono.just(generateMockResponse(request));
                })
                .map(response -> {
                    String imageUrl = (String) response.get("image_url");
                    String enhancedPrompt = (String) response.get("enhanced_prompt");
                    String responseStoryId = (String) response.get("story_id");
                    String responseNodeId = (String) response.get("node_id");

                    log.info("Image generated successfully: {}", imageUrl);
                    log.debug("Enhanced prompt: {}", enhancedPrompt);

                    return ImageGenerationResponseDto.builder()
                            .imageUrl(imageUrl)
                            .enhancedPrompt(enhancedPrompt)
                            .storyId(responseStoryId)
                            .nodeId(responseNodeId)
                            .fileKey(imageUrl) // Using image_url as fileKey for backwards compatibility
                            .generatedAt(Instant.now().toString())
                            .build();
                });
    }

    /**
     * Generate mock response map (for fallback when AI server is unavailable)
     */
    private Map<String, Object> generateMockResponse(ImageGenerationRequestDto request) {
        log.warn("Using MOCK image response (AI-IMAGE server not ready)");

        // Placeholder image URL
        String mockImageUrl = "https://via.placeholder.com/800x600/1a1a1a/ffffff?text=" +
            request.getEpisodeTitle().replaceAll(" ", "+");

        Map<String, Object> response = new HashMap<>();
        response.put("image_url", mockImageUrl);
        response.put("enhanced_prompt", request.getNodeText());
        response.put("story_id", "mock_story");
        response.put("node_id", "mock_node");

        return response;
    }

    /**
     * Check if image generation AI server is healthy
     * Returns a reactive Mono for non-blocking execution
     */
    public Mono<Boolean> checkHealth() {
        log.debug("Image generation AI health check");

        return imageGenerationAiWebClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> {
                    String status = (String) response.get("status");
                    return "running".equals(status);
                })
                .doOnError(e -> log.warn("Image generation AI health check failed: {}", e.getMessage()))
                .onErrorReturn(false);
    }
}
