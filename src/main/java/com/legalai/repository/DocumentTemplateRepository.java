package com.legalai.repository;

import com.legalai.domain.DocumentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {

    List<DocumentTemplate> findByActiveTrue();

    boolean existsByName(String name);
}
