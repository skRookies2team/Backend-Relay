package com.story.relay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Request to index a character for RAG-based chat
 */
@Getter
@Builder
public class CharacterIndexRequestDto {
    @NotBlank(message = "Character ID is required")
    @Size(max = 100, message = "Character ID must not exceed 100 characters")
    private String characterId;

    @NotBlank(message = "Character name is required")
    @Size(max = 200, message = "Character name must not exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 1000, message = "Personality must not exceed 1000 characters")
    private String personality;

    @Size(max = 2000, message = "Background must not exceed 2000 characters")
    private String background;

    private List<String> dialogueSamples;  // 스토리에서 캐릭터의 대사들
    private Map<String, String> relationships;  // 다른 캐릭터와의 관계
    private Map<String, Object> additionalInfo;  // 추가 정보 (외모, 능력 등)
}
