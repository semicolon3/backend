package com.legalai.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LegalDomain domain;

    @Column(nullable = false)
    private boolean archived;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    private Conversation(Long userId, String title, LegalDomain domain) {
        this.userId = userId;
        this.title = title;
        this.domain = domain;
        this.archived = false;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 메시지가 추가되었을 때 호출 — updatedAt을 갱신해 대화방 목록 정렬이 자연스러워진다. */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
