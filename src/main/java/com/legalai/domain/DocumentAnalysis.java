package com.legalai.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DocumentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long documentId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String entitiesJson;

    @Column(columnDefinition = "TEXT")
    private String riskClausesJson;

    /** PDF/OCR로 추출한 원문 텍스트 (문서 뷰어 렌더링용) */
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private DocumentAnalysis(Long documentId, String summary, String entitiesJson, String riskClausesJson, String extractedText) {
        this.documentId = documentId;
        this.summary = summary;
        this.entitiesJson = entitiesJson;
        this.riskClausesJson = riskClausesJson;
        this.extractedText = extractedText;
    }
}
