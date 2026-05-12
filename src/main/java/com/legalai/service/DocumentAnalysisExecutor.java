package com.legalai.service;

import com.legalai.domain.Document;
import com.legalai.domain.DocumentAnalysis;
import com.legalai.domain.ProcessStatus;
import com.legalai.repository.DocumentAnalysisRepository;
import com.legalai.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAnalysisExecutor {

    private final DocumentRepository documentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;

    @Async
    @Transactional
    public void execute(Long documentId) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(documentId).orElse(null);
        if (document == null) return;

        try {
            document.updateStatus(ProcessStatus.PROCESSING, ProcessStatus.PROCESSING);
            documentRepository.save(document);

            // TODO: Claude API 연동 위치
            String summary = String.format("'%s' 문서가 업로드되었습니다. 유형: %s. AI 연동 후 상세 요약이 제공됩니다.",
                    document.getOriginalFileName(), document.getDocumentType());

            String entitiesJson = """
                    {"당사자":["분석 후 추출됩니다."],"날짜":["분석 후 추출됩니다."],"금액":["분석 후 추출됩니다."]}
                    """.strip();

            String riskClausesJson = """
                    [{"clauseTitle":"AI 분석 연동 전","description":"실제 위험 조항은 Claude AI 연동 후 추출됩니다.","severity":"LOW"}]
                    """.strip();

            DocumentAnalysis analysis = DocumentAnalysis.builder()
                    .documentId(documentId)
                    .summary(summary)
                    .entitiesJson(entitiesJson)
                    .riskClausesJson(riskClausesJson)
                    .build();
            documentAnalysisRepository.save(analysis);

            document.updateStatus(ProcessStatus.COMPLETED, ProcessStatus.COMPLETED);
            documentRepository.save(document);

        } catch (Exception e) {
            log.error("문서 분석 실패 - documentId: {}", documentId, e);
            document.updateStatus(ProcessStatus.FAILED, ProcessStatus.FAILED);
            documentRepository.save(document);
        }
    }
}
