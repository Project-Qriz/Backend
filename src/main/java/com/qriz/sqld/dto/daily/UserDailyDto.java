package com.qriz.sqld.dto.daily;

import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.skill.Skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserDailyDto {
    private Long id;
    private String dayNumber;
    private boolean completed;
    private LocalDate planDate;
    private LocalDate completionDate;
    private List<SkillDto> plannedSkills;
    private boolean reviewDay;
    private boolean comprehensiveReviewDay;

    public UserDailyDto(UserDaily userDaily) {
        this.id = userDaily.getId();
        this.dayNumber = userDaily.getDayNumber();
        this.completed = userDaily.isCompleted();
        this.planDate = userDaily.getPlanDate();
        this.completionDate = userDaily.getCompletionDate();
        this.plannedSkills = userDaily.getPlannedSkills().stream()
                .map(SkillDto::new)
                .collect(Collectors.toList());
        this.reviewDay = userDaily.isReviewDay();
        this.comprehensiveReviewDay = userDaily.isComprehensiveReviewDay();
    }

    @Getter
    @Setter
    public static class SkillDto {
        private Long id;
        private String type;
        private String keyConcept;
        private String description;

        public SkillDto(Skill skill) {
            this.id = skill.getId();
            this.type = skill.getType();
            this.keyConcept = skill.getKeyConcepts();
            this.description = skill.getDescription();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyDetailsDto {
        private String dayNumber;
        private boolean passed;
        private List<SkillDetailDto> skills;
        private double totalScore;
        
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SkillDetailDto {
            private Long id;
            private String keyConcepts;
            private String description;    
        }
    }

    @Getter
    @Setter
    @Builder
    public static class TestStatusDto {
        private String dayNumber;
        private int attemptCount;
        private boolean passed;
        private boolean retestEligible;    
    }
}