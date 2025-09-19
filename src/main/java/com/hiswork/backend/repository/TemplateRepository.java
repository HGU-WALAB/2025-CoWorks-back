package com.hiswork.backend.repository;

import com.hiswork.backend.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByCreatedById(String createdById);
    List<Template> findByIsPublicTrue();
} 