package com.story.relay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for learning novel style in AI-IMAGE server
 * Maps to AI-IMAGE server's /api/v1/learn-style endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelStyleLearnRequestDto {

    @NotBlank(message = "Story ID is required")
    private String story_id;

    @NotBlank(message = "Novel text is required")
    private String novel_text;

    private String title;
}
