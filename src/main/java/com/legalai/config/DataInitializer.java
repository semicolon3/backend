package com.legalai.config;

import com.legalai.domain.DocumentTemplate;
import com.legalai.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final DocumentTemplateRepository templateRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initTemplates() {
        seedTemplate(
                "내용증명",
                "상대방에게 특정 사실을 공식적으로 통보하는 법적 문서",
                "법률문서",
                """
                내  용  증  명

                발신인: {발신인_이름}
                주  소: {발신인_주소}
                연락처: {발신인_연락처}

                수신인: {수신인_이름}
                주  소: {수신인_주소}

                제  목: {제목}

                {본문내용}

                위 사항을 내용증명서로 발송하오니 조속한 조치를 취하여 주시기 바랍니다.

                {날짜}

                발신인: {발신인_이름} (인)
                """,
                """
                [
                  {"key":"발신인_이름","label":"발신인 이름","required":true},
                  {"key":"발신인_주소","label":"발신인 주소","required":true},
                  {"key":"발신인_연락처","label":"발신인 연락처","required":true},
                  {"key":"수신인_이름","label":"수신인 이름","required":true},
                  {"key":"수신인_주소","label":"수신인 주소","required":true},
                  {"key":"제목","label":"제목","required":true},
                  {"key":"본문내용","label":"본문 내용","required":true},
                  {"key":"날짜","label":"작성 날짜","required":true}
                ]
                """
        );

        seedTemplate(
                "합의서",
                "분쟁 당사자 간의 합의 내용을 기재한 문서",
                "법률문서",
                """
                합  의  서

                갑: {갑_이름} (주민등록번호: {갑_주민번호})
                   주소: {갑_주소}

                을: {을_이름} (주민등록번호: {을_주민번호})
                   주소: {을_주소}

                위 당사자는 {분쟁내용}에 관하여 다음과 같이 합의합니다.

                제1조 (합의 내용)
                {합의내용}

                제2조 (이행 기한)
                {이행기한}

                제3조 (합의 효력)
                본 합의서는 양 당사자가 서명한 날로부터 효력이 발생하며,
                본 합의 이후 동일한 사안으로 민·형사상 이의를 제기하지 않기로 합니다.

                {날짜}

                갑: {갑_이름} (서명/인)
                을: {을_이름} (서명/인)
                """,
                """
                [
                  {"key":"갑_이름","label":"갑 이름","required":true},
                  {"key":"갑_주민번호","label":"갑 주민등록번호","required":true},
                  {"key":"갑_주소","label":"갑 주소","required":true},
                  {"key":"을_이름","label":"을 이름","required":true},
                  {"key":"을_주민번호","label":"을 주민등록번호","required":true},
                  {"key":"을_주소","label":"을 주소","required":true},
                  {"key":"분쟁내용","label":"분쟁 내용 요약","required":true},
                  {"key":"합의내용","label":"합의 내용","required":true},
                  {"key":"이행기한","label":"이행 기한","required":true},
                  {"key":"날짜","label":"작성 날짜","required":true}
                ]
                """
        );

        seedTemplate(
                "근로계약서",
                "사용자와 근로자 간의 근로 조건을 명시한 계약서",
                "계약서",
                """
                근  로  계  약  서

                사용자(갑): {사용자_이름} (사업자번호: {사업자번호})
                           주소: {사용자_주소}

                근로자(을): {근로자_이름}
                           주소: {근로자_주소}

                갑과 을은 다음과 같이 근로계약을 체결합니다.

                제1조 (근무 장소)  {근무장소}
                제2조 (업무 내용)  {업무내용}
                제3조 (근로 기간)  {근로시작일} ~ {근로종료일}
                제4조 (근무 시간)  {근무시간} (휴게시간: {휴게시간})
                제5조 (임    금)  월 {월급여}원 (지급일: 매월 {급여지급일}일)
                제6조 (연차 휴가)  근로기준법에 따름
                제7조 (사회보험)  4대 보험 적용

                {날짜}

                사용자(갑): {사용자_이름} (인)
                근로자(을): {근로자_이름} (인)
                """,
                """
                [
                  {"key":"사용자_이름","label":"사용자(회사명)","required":true},
                  {"key":"사업자번호","label":"사업자번호","required":true},
                  {"key":"사용자_주소","label":"사용자 주소","required":true},
                  {"key":"근로자_이름","label":"근로자 이름","required":true},
                  {"key":"근로자_주소","label":"근로자 주소","required":true},
                  {"key":"근무장소","label":"근무 장소","required":true},
                  {"key":"업무내용","label":"업무 내용","required":true},
                  {"key":"근로시작일","label":"근로 시작일","required":true},
                  {"key":"근로종료일","label":"근로 종료일 (기간 없을 경우 미기재)","required":false},
                  {"key":"근무시간","label":"근무 시간 (예: 09:00~18:00)","required":true},
                  {"key":"휴게시간","label":"휴게 시간 (예: 12:00~13:00)","required":true},
                  {"key":"월급여","label":"월 급여 (숫자)","required":true},
                  {"key":"급여지급일","label":"급여 지급일 (숫자)","required":true},
                  {"key":"날짜","label":"계약 작성일","required":true}
                ]
                """
        );

        log.info("문서 템플릿 초기화 완료");
    }

    private void seedTemplate(String name, String description, String category,
                               String templateContent, String fieldsJson) {
        if (templateRepository.existsByName(name)) return;

        templateRepository.save(DocumentTemplate.builder()
                .name(name)
                .description(description)
                .category(category)
                .templateContent(templateContent)
                .fieldsJson(fieldsJson)
                .build());
    }
}
