package com.qriz.sqld.domain.apply;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserApplyRepository extends JpaRepository<UserApply, Long> {
    // 사용자가 해당 시험에 접수 중인지 확인
    boolean existsByUserIdAndApplicationId(Long userId, Long applicationId);

    // 사용자가 접수한 시험 정보 조회
    @Query("SELECT ua FROM UserApply ua WHERE ua.user.id = :userId")
    Optional<UserApply> findUserApplyByUserId(@Param("userId") Long userId);
}
