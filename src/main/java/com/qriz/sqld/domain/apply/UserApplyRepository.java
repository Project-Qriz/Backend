package com.qriz.sqld.domain.apply;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserApplyRepository extends JpaRepository<UserApply, Long> {
    boolean existsByUserIdAndApplicationId(Long userId, Long applicationId);
}
