package com.legalai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.domain.Document;
import com.legalai.domain.DocumentAnalysis;
import com.legalai.domain.ProcessStatus;
import com.legalai.repository.DocumentAnalysisRepository;
import com.legalai.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAnalysisExecutor {

    private final DocumentRepository documentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    /** OpenAI 토큰 비용 절감을 위한 본문 최대 길이 (대략 한글 4~5천 자) */
    private static final int MAX_TEXT_LENGTH = 8000;

    @Async
    @Transactional
    public void execute(Long documentId) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(documentId).orElse(null);
        if (document == null) {
            log.warn("분석 대상 문서를 찾을 수 없습니다 - documentId: {}", documentId);
            return;
        }

        try {
            document.updateStatus(ProcessStatus.PROCESSING, ProcessStatus.PROCESSING);
            documentRepository.save(document);

            // 1. 파일에서 텍스트 추출
            String rawText = extractText(document);
            String textForAi = rawText;
            if (textForAi == null || textForAi.isBlank()) {
                // OCR 미도입 단계 — 이미지/카톡 캡쳐는 파일 메타데이터만 전달
                textForAi = String.format(
                        "(텍스트를 추출할 수 없는 파일입니다. 파일명: %s, 유형: %s. " +
                                "이 정보만으로 일반적인 주의사항을 안내해 주세요.)",
                        document.getOriginalFileName(),
                        document.getDocumentType()
                );
            }

            // 2. OpenAI 호출
            String analysisJson = openAiService.analyzeDocument(textForAi, document.getDocumentType());
            log.debug("OpenAI 응답 (documentId={}): {}", documentId, analysisJson);

            // 3. 응답 파싱 → 필드별로 분리해 DB 저장
            JsonNode root = objectMapper.readTree(analysisJson);
            String summary = root.path("summary").asText("");
            String entitiesJson = root.has("entities") ? root.get("entities").toString() : "{}";
            String riskClausesJson = root.has("riskClauses") ? root.get("riskClauses").toString() : "[]";

            DocumentAnalysis analysis = DocumentAnalysis.builder()
                    .documentId(documentId)
                    .summary(summary)
                    .entitiesJson(entitiesJson)
                    .riskClausesJson(riskClausesJson)
                    .extractedText(rawText)   // 원문 텍스트 저장 (뷰어 렌더링용)
                    .build();
            documentAnalysisRepository.save(analysis);

            document.updateStatus(ProcessStatus.COMPLETED, ProcessStatus.COMPLETED);
            documentRepository.save(document);
            log.info("문서 분석 완료 - documentId: {}", documentId);

        } catch (Exception e) {
            log.error("문서 분석 실패 - documentId: {}", documentId, e);
            document.updateStatus(ProcessStatus.FAILED, ProcessStatus.FAILED);
            documentRepository.save(document);
        }
    }

    /**
     * 업로드된 파일에서 분석에 사용할 텍스트를 뽑아온다.
     * - PDF: PDFBox로 텍스트 레이어 추출
     * - 그 외(이미지/카톡 캡쳐 등): OCR 미도입 상태라 null 반환
     */
    private String extractText(Document document) {
        String mime = document.getMimeType() == null ? "" : document.getMimeType().toLowerCase();
        File file = new File(document.getFilePath());

        if (!file.exists()) {
            log.warn("파일이 존재하지 않습니다 - path: {}", document.getFilePath());
            return null;
        }

        if (mime.contains("pdf")) {
            try (PDDocument pdf = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(pdf);
                if (text == null) return null;
                if (text.length() > MAX_TEXT_LENGTH) {
                    text = text.substring(0, MAX_TEXT_LENGTH) + "\n... (이하 생략)";
                }
                return text;
            } catch (Exception e) {
                log.error("PDF 텍스트 추출 실패 - documentId: {}", document.getId(), e);
                return null;
            }
        }

        // 이미지/캡쳐 등은 OCR 도입 후 처리. 현재는 분석기가 메타데이터로 일반 가이드만 생성.
        return null;
    }
}
