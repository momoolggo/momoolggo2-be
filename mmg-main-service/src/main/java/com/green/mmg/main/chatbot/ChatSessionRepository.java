package com.green.mmg.main.chatbot;

import com.green.mmg.main.chatbot.entity.ChatSession;
import com.green.mmg.main.chatbot.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserNoOrderBySessionIdDesc(Long userNo);
    List<ChatSession> findByStatus(SessionStatus status);
}
