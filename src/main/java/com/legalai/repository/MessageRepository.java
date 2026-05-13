package com.legalai.repository;

import com.legalai.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** 최근 N개 메시지를 시간 역순으로 — 프롬프트 컨텍스트 구성용 */
    List<Message> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
