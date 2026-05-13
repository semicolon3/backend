package com.legalai.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_conversation_id", columnList = "conversationId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageRole role;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 답변에 인용된 법령·판례 메타데이터 (JSON 배열 문자열).
     * 예: [{"lawId":"001132","lawName":"주택임대차보호법","article":"제7조의2"}]
     * 사용자 메시지는 null.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String citationsJson;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Message(Long conversationId, MessageRole role, String content, String citationsJson) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.citationsJson = citationsJson;
    }
}
