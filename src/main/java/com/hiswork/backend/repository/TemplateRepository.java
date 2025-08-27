package com.hiswork.backend.repository;

import com.hiswork.backend.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByCreatedById(UUID createdById);
    
    List<Template> findByIsPublicTrue();
} 