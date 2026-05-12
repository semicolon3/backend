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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskClause {
        private String clauseTitle;
        private String description;
        private String severity; // HIGH, MEDIUM, LOW
    }
}
