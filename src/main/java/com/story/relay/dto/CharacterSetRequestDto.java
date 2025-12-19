package com.story.relay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for setting/updating character information
 * Maps to NPC AI server's /api/ai/character endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSetRequestDto {

    @NotBlank(message = "Character ID is required")
    private String characterId;  // Maps to session_id in NPC AI

    @NotBlank(message = "Character name is required")
    private String characterName;

    private String characterDescription;  // Optional: character traits, personality, etc.
}
