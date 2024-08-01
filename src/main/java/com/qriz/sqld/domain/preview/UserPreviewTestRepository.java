package com.qriz.sqld.domain.preview;

import com.qriz.sqld.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPreviewTestRepository extends JpaRepository<UserPreviewTest, Long> {
    List<UserPreviewTest> findByUser(User user);
    boolean existsByUserAndCompleted(User user, boolean completed);
    List<UserPreviewTest> findByUserIdAndCompleted(Long userId, boolean completed);
}