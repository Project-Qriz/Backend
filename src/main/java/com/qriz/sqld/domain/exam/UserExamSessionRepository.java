package com.qriz.sqld.domain.exam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserExamSessionRepository extends JpaRepository<UserExamSession, Long> {

        /**
         * 사용자 ID와 세션 정보로 UserExamSession을 찾는 메소드
         *
         * @param userId  사용자 ID
         * @param session 세션 정보
         * @return 해당하는 UserExamSession, 없으면 Optional.empty()
         */
        Optional<UserExamSession> findByUserIdAndSession(Long userId, String session);

        List<UserExamSession> findByUserIdAndSessionOrderByCompletionDateAsc(Long userId, String session);

        @Query("SELECT u FROM UserExamSession u " +
                        "WHERE u.user.id = :userId AND u.session = :session " +
                        "ORDER BY u.completionDate DESC, u.id DESC")
        List<UserExamSession> findByUserIdAndSessionOrderByCompletionDateDesc(
                        @Param("userId") Long userId,
                        @Param("session") String session);

        // 진행 중인(미완료) 세션을 찾는 메서드
        Optional<UserExamSession> findByUserIdAndSessionAndCompletionDateIsNull(Long userId, String session);

        // 완료된 세션 수를 카운트하는 메서드
        long countByUserIdAndSessionAndCompletionDateIsNotNull(Long userId, String session);

        List<UserExamSession> findByUserIdAndSessionAndCompletionDateBetween(
                        Long userId,
                        String session,
                        LocalDateTime startDate,
                        LocalDateTime endDate);
}
