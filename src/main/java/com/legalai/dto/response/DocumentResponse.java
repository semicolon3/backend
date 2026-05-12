package com.legalai.dto.response;

import com.legalai.domain.Document;
import com.legalai.domain.DocumentType;
import com.legalai.domain.ProcessStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentResponse {

    private Long id;
    private String originalFileName;
    private Long fileSize;
    private String mimeType;
    private DocumentType documentType;
    private ProcessStatus ocrStatus;
    private ProcessStatus analysisStatus;
    private LocalDateTime createdAt;

    public static DocumentResponse from(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .originalFileName(document.getOriginalFileName())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .documentType(document.getDocumentType())
                .ocrStatus(document.getOcrStatus())
                .analysisStatus(document.getAnalysisStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
