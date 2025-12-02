package com.story.relay.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response from RAG-based character chatbot
 */
@Getter
@Builder
public class ChatMessageResponseDto {
    private String characterId;
    private String aiMessage;
    private List<RagSource> sources;  // RAG에서 참조한 소스
    private String timestamp;

    @Getter
    @Builder
    public static class RagSource {
        private String text;
        private Double score;
        private String sourceType;  // "dialogue", "description", "personality" 등
    }
}
