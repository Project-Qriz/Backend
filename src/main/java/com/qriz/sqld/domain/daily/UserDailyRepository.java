package com.qriz.sqld.domain.daily;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.qriz.sqld.domain.user.User;

@Repository
public interface UserDailyRepository extends JpaRepository<UserDaily, Long> {
    Optional<UserDaily> findLatestDailyByCompletion(User user, boolean completed);
    Optional<UserDaily> findLatestDaily(User user);
    Optional<UserDaily> findDailyByUserAndDay(User user, String dayNumber);
}
