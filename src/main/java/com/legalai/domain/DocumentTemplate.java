package com.legalai.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DocumentTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String templateContent;

    // 입력 필드 목록 JSON (예: [{"key":"발신인_이름","label":"발신인 이름","required":true}])
    @Column(columnDefinition = "TEXT")
    private String fieldsJson;

    @Column(nullable = false)
    private boolean active;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private DocumentTemplate(String name, String description, String category,
                              String templateContent, String fieldsJson) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.templateContent = templateContent;
        this.fieldsJson = fieldsJson;
        this.active = true;
    }
}
