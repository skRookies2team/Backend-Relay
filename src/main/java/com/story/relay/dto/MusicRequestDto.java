package com.story.relay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for music recommendation from AI-BGM server
 * Maps to AI-BGM server's /api/analyze endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicRequestDto {

    @NotBlank(message = "Prompt is required")
    private String prompt;  // Scene description for music recommendation
}
