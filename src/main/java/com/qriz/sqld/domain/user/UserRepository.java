package com.qriz.sqld.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 아이디로 찾기
    Optional<User> findByUsername(String username);

    // 이메일로 찾기
    Optional<User> findByEmail(String email);

    // 닉네임과 이메일로 아이디 찾기
    Optional<User> findByNicknameAndEmail(String nickname, String email);

    @Modifying
    @Query("DELETE FROM User u WHERE u = :user")
    void delete(@Param("user") User user);
}
