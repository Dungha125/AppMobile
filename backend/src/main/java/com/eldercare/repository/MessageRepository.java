package com.eldercare.repository;

import com.eldercare.model.Conversation;
import com.eldercare.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);
}

