package com.qriz.sqld.dto.recommend;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class WeeklyRecommendationDto {
    private String recommendationType;
    private List<ConceptRecommendation> recommendations;

    @Getter
    @Setter
    @Builder
    public static class ConceptRecommendation {
        private Long skillId;
        private String keyConcepts;
        private String description;
        private Integer frequency;
        private Double incorrectRate;  // 프리뷰 테스트를 실시한 경우에만 사용
    }
}
