package com.legalai.dto.response;

import com.legalai.domain.ProcessStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DocumentAnalysisResponse {

    private Long documentId;
    private ProcessStatus analysisStatus;
    private String summary;
    private Map<String, List<String>> entities;
    private List<RiskClause> riskClauses;
    /** PDF/OCR로 추출한 원문 전체 텍스트 (프론트 문서 뷰어 렌더링용) */
    private String extractedText;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskClause {
        private String clauseTitle;
        /** 문서 원문에서 해당 조항의 실제 텍스트 (프론트 하이라이팅용) */
        private String originalText;
        private String description;
        private String severity; // HIGH, MEDIUM, LOW
    }
}
