package com.hiswork.backend.repository;

import com.hiswork.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    List<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name);
} 