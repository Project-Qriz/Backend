package com.qriz.sqld.domain.userActivity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.user.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long id;

    /**
     * 사용자 외래 키
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 문제 외래 키
     */
    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    /**
     * 테스트 정보
     * (Preview Test, dayNumber)
     */
    private String testInfo;

    /**
     * 문제 번호
     */
    private int questionNum;

    /**
     * 사용자가 체크한 정답
     */
    private String checked;

    /**
     * 소요 시간
     */
    private Integer timeSpent;

    /**
     * 정답 여부 (false: 오답, true: 정답)
     */
    private boolean correction;

    /**
     * 점수
     */
    private Double score;

    /**
     * 푼 날짜
     */
    private LocalDateTime date;
}