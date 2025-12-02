package com.story.relay.dto;

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
    private String characterId;
    private String name;
    private String description;
    private String personality;
    private String background;
    private List<String> dialogueSamples;  // 스토리에서 캐릭터의 대사들
    private Map<String, String> relationships;  // 다른 캐릭터와의 관계
    private Map<String, Object> additionalInfo;  // 추가 정보 (외모, 능력 등)
}
