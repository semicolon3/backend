package com.legalai.domain;

/**
 * 4대 핵심 법률 도메인 + 기타.
 * 대화방 생성 시 사용자가 선택하면 그 분야에 특화된 RAG 검색이 수행된다.
 */
public enum LegalDomain {
    LEASE,     // 임대차 (보증금·수리비·퇴거·갱신)
    LABOR,     // 근로 (임금체불·부당해고·4대보험)
    CONSUMER,  // 소비자 (온라인 사기·환불·약관)
    TRAFFIC,   // 교통 (사고·과실비율·합의)
    OTHER      // 기타
}
