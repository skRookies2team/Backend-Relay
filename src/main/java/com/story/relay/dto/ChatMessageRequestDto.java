package com.story.relay.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Request to send a message to a character chatbot
 */
@Getter
@Builder
public class ChatMessageRequestDto {
    private String characterId;
    private String userMessage;
    private List<ConversationMessage> conversationHistory;  // 대화 이력
    private Integer maxTokens;  // 응답 최대 토큰 수

    @Getter
    @Builder
    public static class ConversationMessage {
        private String role;  // "user" or "assistant"
        private String content;
    }
}
