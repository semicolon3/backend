package com.legalai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.domain.*;
import com.legalai.dto.response.DocumentAnalysisResponse;
import com.legalai.dto.response.DocumentResponse;
import com.legalai.exception.CustomException;
import com.legalai.exception.ErrorCode;
import com.legalai.repository.DocumentAnalysisRepository;
import com.legalai.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final FileStorageService fileStorageService;
    private final DocumentAnalysisExecutor analysisExecutor;
    private final ObjectMapper objectMapper;

    @Transactional
    public DocumentResponse upload(MultipartFile file, DocumentType documentType, Long userId) {
        String storedFileName = fileStorageService.store(file);

        Document document = Document.builder()
                .userId(userId)
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .filePath(fileStorageService.getFilePath(storedFileName))
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .documentType(documentType)
                .build();
        documentRepository.save(document);

        // 별도 빈에서 호출해야 @Async 정상 동작
        Long documentId = document.getId();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        analysisExecutor.execute(documentId);
                    }
                });

        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(Long userId) {
        return documentRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long documentId, Long userId) {
        return DocumentResponse.from(findDocument(documentId, userId));
    }

    @Transactional(readOnly = true)
    public DocumentAnalysisResponse getAnalysis(Long documentId, Long userId) {
        Document document = findDocument(documentId, userId);

        if (document.getAnalysisStatus() != ProcessStatus.COMPLETED) {
            return DocumentAnalysisResponse.builder()
                    .documentId(documentId)
                    .analysisStatus(document.getAnalysisStatus())
                    .build();
        }

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        try {
            Map<String, List<String>> entities = objectMapper.readValue(
                    analysis.getEntitiesJson(), new TypeReference<>() {});
            List<DocumentAnalysisResponse.RiskClause> riskClauses = objectMapper.readValue(
                    analysis.getRiskClausesJson(), new TypeReference<>() {});

            return DocumentAnalysisResponse.builder()
                    .documentId(document.getId())
                    .analysisStatus(document.getAnalysisStatus())
                    .summary(analysis.getSummary())
                    .entities(entities)
                    .riskClauses(riskClauses)
                    .build();
        } catch (Exception e) {
            log.error("분석 결과 파싱 실패 - documentId: {}", documentId, e);
            return DocumentAnalysisResponse.builder()
                    .documentId(documentId)
                    .analysisStatus(ProcessStatus.FAILED)
                    .build();
        }
    }

    @Transactional
    public void delete(Long documentId, Long userId) {
        Document document = findDocument(documentId, userId);
        fileStorageService.delete(document.getStoredFileName());
        document.softDelete();
    }

    private Document findDocument(Long documentId, Long userId) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
        if (!document.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        return document;
    }
}
