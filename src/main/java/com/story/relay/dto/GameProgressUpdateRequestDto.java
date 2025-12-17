package com.story.relay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 게임 진행 상황 업데이트 요청 DTO
 * Backend → Relay → NPC AI로 현재 게임 상황 전달
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameProgressUpdateRequestDto {
    
    @NotBlank(message = "Character ID is required")
    @Size(max = 100, message = "Character ID must not exceed 100 characters")
    private String characterId;  // session_id로 사용
    
    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;  // 현재 노드 정보를 텍스트로 변환
    
    private Map<String, Object> metadata;  // 추가 메타데이터
}
