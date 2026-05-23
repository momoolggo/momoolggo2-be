package com.green.mmg.main.chatbot;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.chatbot.dto.ChatMessageRes;
import com.green.mmg.main.chatbot.dto.ChatSendRes;
import com.green.mmg.main.chatbot.dto.ChatSessionRes;
import com.green.mmg.main.chatbot.entity.*;
import com.green.mmg.main.pet.PetService;
import com.green.mmg.main.pet.entity.Pet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final PetService petService;
    private final GeminiClient geminiClient;
    private final ChatbotPromptBuilder promptBuilder;

    /** 옵션 주입 — P-4/P-5 컨텍스트 확장 시 별 @Component 등록되면 자동 주입. */
    @Autowired(required = false)
    private ChatbotContextProvider contextProvider;

    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 512;
    private static final String GEMINI_FALLBACK_MESSAGE =
            "지금은 답변을 드리기 어려워요. 잠시 후 다시 시도해주세요.";

    @Transactional
    public ChatSessionRes startSession(Long userNo, EntryPoint entryPoint, ToneMode toneMode) {
        if (entryPoint == null) {
            throw new BusinessException("entryPoint가 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        Long petNo = null;
        if (entryPoint == EntryPoint.MYPET) {
            Pet pet = petService.getOrCreatePet(userNo);
            petNo = pet.getPetNo();
        }
        ChatSession session = sessionRepository.save(new ChatSession(userNo, petNo, entryPoint, toneMode));
        return new ChatSessionRes(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionRes> listMySessions(Long userNo) {
        return sessionRepository.findByUserNoOrderBySessionIdDesc(userNo)
                .stream().map(ChatSessionRes::new).toList();
    }

    @Transactional
    public ChatSendRes sendMessage(Long callerUserNo, Long sessionId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("메시지 내용이 비어있습니다.", HttpStatus.BAD_REQUEST);
        }
        ChatSession session = loadOwnedSession(callerUserNo, sessionId);
        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new BusinessException("종료된 세션입니다.", HttpStatus.BAD_REQUEST);
        }

        ChatMessage userMsg = messageRepository.save(
                new ChatMessage(sessionId, MessageRole.USER, content));

        String assistantText;
        if (session.getStatus() == SessionStatus.ESCALATED) {
            assistantText = "상담원 연결 대기 중입니다. 잠시만 기다려주세요.";
        } else {
            assistantText = callGemini(session, content);
        }
        ChatMessage assistantMsg = messageRepository.save(
                new ChatMessage(sessionId, MessageRole.ASSISTANT, assistantText));

        return new ChatSendRes(
                new ChatMessageRes(userMsg),
                new ChatMessageRes(assistantMsg),
                session.getStatus());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageRes> listMessages(Long callerUserNo, Long sessionId) {
        loadOwnedSession(callerUserNo, sessionId);
        return messageRepository.findBySessionIdOrderByMessageIdAsc(sessionId)
                .stream().map(ChatMessageRes::new).toList();
    }

    @Transactional
    public ChatSessionRes escalate(Long callerUserNo, Long sessionId) {
        ChatSession session = loadOwnedSession(callerUserNo, sessionId);
        if (session.getEntryPoint() != EntryPoint.CS) {
            throw new BusinessException("CS 세션만 상담원 연결이 가능합니다.", HttpStatus.BAD_REQUEST);
        }
        session.escalate();
        return new ChatSessionRes(session);
    }

    @Transactional
    public ChatSessionRes closeSession(Long callerUserNo, Long sessionId) {
        ChatSession session = loadOwnedSession(callerUserNo, sessionId);
        session.close();
        return new ChatSessionRes(session);
    }

    private ChatSession loadOwnedSession(Long callerUserNo, Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(session.getUserNo(), callerUserNo)) {
            throw new BusinessException("권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return session;
    }

    private String callGemini(ChatSession session, String userContent) {
        try {
            Pet pet = (session.getPetNo() != null)
                    ? petService.getOrCreatePet(session.getUserNo())
                    : null;
            String extraContext = Optional.ofNullable(contextProvider)
                    .map(p -> p.buildContext(pet))
                    .orElse(null);
            String systemInstruction = promptBuilder.buildSystemInstruction(
                    session.getEntryPoint(), session.getToneMode(), pet, extraContext);
            return geminiClient.generateText(systemInstruction, userContent, DEFAULT_MAX_OUTPUT_TOKENS);
        } catch (GeminiException e) {
            log.warn("Gemini 호출 실패 — sessionId={}, cause={}", session.getSessionId(), e.getMessage());
            return GEMINI_FALLBACK_MESSAGE;
        }
    }
}
