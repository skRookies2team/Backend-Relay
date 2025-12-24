package com.story.relay.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Request to send a message to a character chatbot
 */
@Getter
@Builder
public class ChatMessageRequestDto {
    @NotBlank(message = "Character ID is required")
    @Size(max = 100, message = "Character ID must not exceed 100 characters")
    private String characterId;

    @Size(max = 100, message = "Character name must not exceed 100 characters")
    private String characterName;  // 캐릭터 이름 (RAG 서버로 전달)

    @NotBlank(message = "User message is required")
    @Size(max = 2000, message = "User message must not exceed 2000 characters")
    private String userMessage;

    @Valid
    private List<ConversationMessage> conversationHistory;  // 대화 이력

    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 4000, message = "Max tokens must not exceed 4000")
    private Integer maxTokens;  // 응답 최대 토큰 수

    @Getter
    @Builder
    public static class ConversationMessage {
        @NotBlank(message = "Role is required")
        @Size(max = 20, message = "Role must not exceed 20 characters")
        private String role;  // "user" or "assistant"

        @NotBlank(message = "Content is required")
        @Size(max = 2000, message = "Content must not exceed 2000 characters")
        private String content;
    }
}
