package com.qriz.sqld.domain.survey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.user.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    // 설문 조사에서 체크했는지 여부
    private boolean checked;

    // 설문조사에서 아무것도 모른다 선택
    @Column(name = "knows_nothing")
    private boolean knowsNothing;

    public Survey(User user, Skill skill, boolean checked) {
        this.user = user;
        this.skill = skill;
        this.checked = checked;
        this.knowsNothing = false;
    }

    // "아무것도 모른다" 옵션을 위한 정적 팩토리 메소드
    public static Survey createKnowsNothingSurvey(User user) {
        Survey survey = new Survey();
        survey.setUser(user);
        survey.setSkill(null);
        survey.setChecked(false);
        survey.setKnowsNothing(true);
        return survey;
    }
}