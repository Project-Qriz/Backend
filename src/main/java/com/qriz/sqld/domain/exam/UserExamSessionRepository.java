package com.qriz.sqld.domain.exam;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserExamSessionRepository extends JpaRepository<UserExamSession, Long> {
    
    /**
     * 사용자 ID와 세션 정보로 UserExamSession을 찾는 메소드
     *
     * @param userId 사용자 ID
     * @param session 세션 정보
     * @return 해당하는 UserExamSession, 없으면 Optional.empty()
     */
    Optional<UserExamSession> findByUserIdAndSession(Long userId, String session);
}
