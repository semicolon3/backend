package com.legalai.repository;

import com.legalai.domain.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Optional<Conversation> findByIdAndDeletedAtIsNull(Long id);
}
