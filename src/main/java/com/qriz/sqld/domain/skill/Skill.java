package com.qriz.sqld.domain.skill;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.qriz.sqld.domain.survey.Survey;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class Skill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long id;

    /**
     * 1과목 / 2과목
     */
    private String title;

    /**
     * 주요 항목
     */
    private String type;

    /**
     * 세부 항목
     */
    private String keyConcepts;

    /**
     * 출제 빈도
     */
    private Integer frequency;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Survey> surveys;
}
