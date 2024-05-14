package com.qriz.sqld.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 아이디로 찾기
    Optional<User> findByUsername(String username);

    // 이메일로 찾기
    Optional<User> findByEmail(String email);

    // 닉네임과 이메일로 아이디 찾기
    Optional<User> findByNicknameAndEmail(String nickname, String email);

    // 아이디와 이메일로 계정 존재 확인
    Optional<User> findByUsernameAndEmail(String username, String email);
}
