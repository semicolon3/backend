package com.legalai.repository;

import com.legalai.domain.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, Long> {

    Optional<DocumentAnalysis> findByDocumentId(Long documentId);
}
