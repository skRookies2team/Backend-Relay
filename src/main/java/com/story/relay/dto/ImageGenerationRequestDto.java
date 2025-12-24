package com.story.relay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageGenerationRequestDto {
    private String storyId;

    private String nodeId;

    @NotBlank(message = "Node text is required")
    @Size(max = 1000, message = "Node text must not exceed 1000 characters")
    private String nodeText;

    @Size(max = 200, message = "Situation must not exceed 200 characters")
    private String situation;

    private Map<String, String> npcEmotions;

    @NotBlank(message = "Episode title is required")
    @Size(max = 200, message = "Episode title must not exceed 200 characters")
    private String episodeTitle;

    @NotNull(message = "Episode order is required")
    @Min(value = 0, message = "Episode order must be non-negative")
    private Integer episodeOrder;

    @Min(value = 0, message = "Node depth must be non-negative")
    private Integer nodeDepth;

    @Size(max = 100, message = "Image style must not exceed 100 characters")
    private String imageStyle;

    @Size(max = 500, message = "Additional context must not exceed 500 characters")
    private String additionalContext;

    @Builder.Default
    private Boolean generateImage = true;

    private String novelS3Bucket;

    private String novelS3Key;

    private String imageS3Url;  // 이미지 업로드용 S3 presigned URL (AI-IMAGE 서버로 전달)
}
