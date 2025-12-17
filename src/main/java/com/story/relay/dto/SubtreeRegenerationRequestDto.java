package com.story.relay.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 서브트리 재생성 요청 DTO (Relay Server → Python AI)
 */
@Getter
@Builder
public class SubtreeRegenerationRequestDto {
    @NotBlank(message = "Episode title is required")
    @Size(max = 200, message = "Episode title must not exceed 200 characters")
    private String episodeTitle;

    @NotNull(message = "Episode order is required")
    @Min(value = 0, message = "Episode order must be non-negative")
    private Integer episodeOrder;

    @NotNull(message = "Parent node is required")
    @Valid
    private ParentNodeInfo parentNode;

    @NotNull(message = "Current depth is required")
    @Min(value = 0, message = "Current depth must be non-negative")
    private Integer currentDepth;

    @NotNull(message = "Max depth is required")
    @Min(value = 1, message = "Max depth must be at least 1")
    private Integer maxDepth;

    // Novel context is optional when cached data (summary, charactersJson, gaugesJson) is provided
    @Size(max = 10000, message = "Novel context must not exceed 10000 characters")
    private String novelContext;

    private List<String> previousChoices;
    private List<String> selectedGaugeIds;

    // 캐싱된 분석 결과 (성능 최적화)
    private String summary;                     // 소설 요약 (캐시)
    private String charactersJson;              // 캐릭터 정보 (캐시)
    private String gaugesJson;                  // 게이지 정보 (캐시)

    @Getter
    @Builder
    public static class ParentNodeInfo {
        @NotBlank(message = "Node ID is required")
        @Size(max = 100, message = "Node ID must not exceed 100 characters")
        private String nodeId;

        @NotBlank(message = "Node text is required")
        @Size(max = 1000, message = "Node text must not exceed 1000 characters")
        private String text;

        private List<String> choices;

        @Size(max = 200, message = "Situation must not exceed 200 characters")
        private String situation;

        private Map<String, String> npcEmotions;
        private List<String> tags;

        @NotNull(message = "Depth is required")
        @Min(value = 0, message = "Depth must be non-negative")
        private Integer depth;
    }
}
