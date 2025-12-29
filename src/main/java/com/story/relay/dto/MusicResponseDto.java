package com.story.relay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for music recommendation from AI-BGM server
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicResponseDto {

    private AnalysisData analysis;
    private MusicData music;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisData {
        private String primary_mood;
        private String secondary_mood;
        private Double intensity;
        private List<String> emotional_tags;
        private String reasoning;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MusicData {
        private String mood;
        private String filename;
        private String file_path;
        private String streaming_url;
    }
}
