package com.hiswork.backend.repository;

import com.hiswork.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    // 이메일로 사용자 찾기
    Optional<User> findByEmail(String email);
    
    // 이름으로 사용자 찾기
    Optional<User> findByName(String name);
    
    // 이메일 또는 이름으로 사용자 찾기 (편집자/검토자 할당시 유연한 검색을 위해)
    @Query("SELECT u FROM User u WHERE u.email = :emailOrName OR u.name = :emailOrName")
    Optional<User> findByEmailOrName(@Param("emailOrName") String emailOrName);
    
    // 이메일 또는 이름으로 사용자 검색 (대소문자 무시)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(@Param("query") String query1, @Param("query") String query2);
}