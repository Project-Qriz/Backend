package com.qriz.sqld.domain.preview;

import com.qriz.sqld.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreviewTestRepository extends JpaRepository<UserPreviewTest, Long> {
    List<UserPreviewTest> findByUser(User user);

    boolean existsByUserAndCompleted(User user, boolean completed);

    List<UserPreviewTest> findByUserIdAndCompleted(Long userId, boolean completed);

    Optional<UserPreviewTest> findFirstByUserAndCompletedOrderByCompletionDateDesc(User user, boolean completed);

    @Modifying
    @Query("DELETE FROM UserPreviewTest upt WHERE upt.user = :user")
    void deleteByUser(@Param("user") User user);
}