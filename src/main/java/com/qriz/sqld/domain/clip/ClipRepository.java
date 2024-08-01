package com.qriz.sqld.domain.clip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClipRepository extends JpaRepository<Clipped, Long> {
    List<Clipped> findByUserActivity_User_Id(Long userId);
    Optional<Clipped> findByUserActivity_Id(Long activityId);
    boolean existsByUserActivity_Id(Long activityId);
}
