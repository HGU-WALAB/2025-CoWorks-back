package com.hiswork.backend.repository;

import com.hiswork.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUniqueId(String uniqueId);
    List<User> findByUniqueIdContainingIgnoreCaseOrNameContainingIgnoreCase(String uniqueId, String name);
} 