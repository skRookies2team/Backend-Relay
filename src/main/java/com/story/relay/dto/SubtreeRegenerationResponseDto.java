package com.story.relay.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 서브트리 재생성 응답 DTO (Python AI → Relay Server)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtreeRegenerationResponseDto {
    private String status;
    private String message;
    private List<RegeneratedNode> regeneratedNodes;
    private Integer totalNodesRegenerated;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegeneratedNode {
        @JsonProperty("id")
        private String nodeId;

        private String text;
        private List<ChoiceDto> choices;
        private Integer depth;
        private String parentId;
        private NodeDetails details;

        @JsonProperty("children")
        private List<RegeneratedNode> children;  // ✅ 재귀적 구조 지원

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NodeDetails {
            private String situation;
            
            @JsonProperty("npcEmotions")
            private Map<String, String> npcEmotions;
            
            private List<String> tags;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChoiceDto {
        private String text;
        private List<String> tags;
        
        @JsonAlias("immediate_reaction")
        private String immediateReaction;
    }
}
