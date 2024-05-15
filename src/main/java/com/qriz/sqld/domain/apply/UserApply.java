package com.qriz.sqld.domain.apply;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qriz.sqld.domain.application.Application;
import com.qriz.sqld.domain.user.User;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class UserApply {
    
    @Id
    @Column(name = "user_apply_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonFormat(pattern = "yyyy년 MM월 dd일 HH시 mm분")
    private LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "apply_id")
    private Application application;

    public UserApply(User user, Application application) {
        this.date = LocalDateTime.now();
        this.user = user;
        this.application = application;
    }
}
