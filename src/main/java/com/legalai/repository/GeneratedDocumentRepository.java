package com.legalai.repository;

import com.legalai.domain.GeneratedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {

    List<GeneratedDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GeneratedDocument> findByIdAndUserId(Long id, Long userId);
}
