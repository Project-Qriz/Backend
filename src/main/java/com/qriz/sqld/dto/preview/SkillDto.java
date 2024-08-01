package com.qriz.sqld.dto.preview;

import com.qriz.sqld.domain.skill.Skill;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillDto {
    private Long id;
    private String title;
    private String type;
    private String keyConcepts;
    private Integer frequency;
    private String description;

    public static SkillDto from(Skill skill) {
        SkillDto dto = new SkillDto();
        dto.setId(skill.getId());
        dto.setTitle(skill.getTitle());
        dto.setType(skill.getType());
        dto.setKeyConcepts(skill.getKeyConcepts());
        dto.setFrequency(skill.getFrequency());
        dto.setDescription(skill.getDescription());
        return dto;
    }
}
