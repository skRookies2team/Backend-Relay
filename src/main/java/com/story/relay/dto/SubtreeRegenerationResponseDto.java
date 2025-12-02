package com.story.relay.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 서브트리 재생성 응답 DTO (Python AI → Relay Server)
 */
@Getter
@Builder
public class SubtreeRegenerationResponseDto {
    private String status;
    private String message;
    private List<RegeneratedNode> regeneratedNodes;
    private Integer totalNodesRegenerated;

    @Getter
    @Builder
    public static class RegeneratedNode {
        private String nodeId;
        private String text;
        private List<String> choices;
        private Integer depth;
        private String parentId;
        private NodeDetails details;

        @Getter
        @Builder
        public static class NodeDetails {
            private String situation;
            private Map<String, String> npcEmotions;
            private List<String> tags;
        }
    }
}
