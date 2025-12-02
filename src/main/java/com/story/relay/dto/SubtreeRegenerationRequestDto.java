package com.story.relay.dto;

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
    private String episodeTitle;
    private Integer episodeOrder;
    private ParentNodeInfo parentNode;
    private Integer currentDepth;
    private Integer maxDepth;
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
        private String nodeId;
        private String text;
        private List<String> choices;
        private String situation;
        private Map<String, String> npcEmotions;
        private List<String> tags;
        private Integer depth;
    }
}
