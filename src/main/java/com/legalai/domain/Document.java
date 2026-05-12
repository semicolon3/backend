package com.legalai.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String filePath;

    private Long fileSize;

    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessStatus ocrStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessStatus analysisStatus;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    private Document(Long userId, String originalFileName, String storedFileName,
                     String filePath, Long fileSize, String mimeType, DocumentType documentType) {
        this.userId = userId;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.documentType = documentType;
        this.ocrStatus = ProcessStatus.PENDING;
        this.analysisStatus = ProcessStatus.PENDING;
    }

    public void updateStatus(ProcessStatus ocrStatus, ProcessStatus analysisStatus) {
        this.ocrStatus = ocrStatus;
        this.analysisStatus = analysisStatus;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
