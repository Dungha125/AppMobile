package com.eldercare.repository;

import com.eldercare.model.Conversation;
import com.eldercare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByElderlyAndCaregiver(User elderly, User caregiver);

    List<Conversation> findByElderly(User elderly);

    List<Conversation> findByCaregiver(User caregiver);
}

