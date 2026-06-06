package com.green.mmg.main.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.chatbot.dto.ChatMessageRes;
import com.green.mmg.main.chatbot.dto.ChatRecommendation;
import com.green.mmg.main.chatbot.dto.ChatSendRes;
import com.green.mmg.main.chatbot.dto.ChatSessionRes;
import com.green.mmg.main.chatbot.dto.MenuCardDto;
import com.green.mmg.main.chatbot.entity.*;
import com.green.mmg.main.feign.AdminFeignClient;
import com.green.mmg.main.pet.PetService;
import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.store.StoreMapper;
import com.green.mmg.main.store.model.StoreGetRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final AdminFeignClient adminFeignClient;  // P-7 에스컬레이션
    private final StoreMapper storeMapper;  // #1+#2 메뉴 추천 키워드 검색 (2026-06-06)
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 옵션 주입 — P-4/P-5 컨텍스트 확장 시 별 @Component 등록되면 자동 주입. */
    @Autowired(required = false)
    private ChatbotContextProvider contextProvider;

    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 800;
    // 2026-06-06 메뉴 추천 강화 — 키워드 5개 / 카드 8개 / 키워드당 2개씩 분산
    private static final int MAX_CARDS = 8;
    private static final int MAX_STORES_PER_KEYWORD = 2;
    private static final String GEMINI_FALLBACK_MESSAGE =
            "지금은 답변을 드리기 어려워요. 잠시 후 다시 시도해주세요.";

    /** Gemini responseSchema — {message, menuKeywords[]}. maxItems 3→5 (2026-06-06). */
    private static final String RECOMMEND_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "message": { "type": "string" },
                "menuKeywords": {
                  "type": "array",
                  "items": { "type": "string" },
                  "maxItems": 5
                }
              },
              "required": ["message"],
              "propertyOrdering": ["message", "menuKeywords"]
            }
            """;

    @Transactional
    public ChatSessionRes startSession(Long userNo, String callerRole, EntryPoint entryPoint, ToneMode toneMode) {
        if (entryPoint == null) {
            throw new BusinessException("entryPoint가 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        Long petNo = null;
        if (entryPoint == EntryPoint.MYPET) {
            // 자잘 에러 트랙(2026-05-23) — 펫 챗봇은 CUSTOMER 전용 (사장/라이더 펫 자동 생성 side effect 차단).
            if (!"CUSTOMER".equals(callerRole)) {
                throw new BusinessException(
                        "펫 챗봇은 고객 전용입니다. 사장/라이더는 CS 챗봇만 사용 가능합니다.",
                        HttpStatus.FORBIDDEN);
            }
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
    public ChatSendRes sendMessage(Long callerUserNo, String callerRole, Long sessionId, String content) {
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
        List<MenuCardDto> menuCards = List.of();
        if (session.getStatus() == SessionStatus.ESCALATED) {
            assistantText = "상담원 연결 대기 중입니다. 잠시만 기다려주세요.";
        } else {
            ChatRecommendation rec = callGemini(session, callerRole, content);
            assistantText = rec.getMessage();
            if (!rec.getMenuKeywords().isEmpty()) {
                menuCards = searchMenuCards(rec.getMenuKeywords());
            }
        }
        ChatMessage assistantMsg = messageRepository.save(
                new ChatMessage(sessionId, MessageRole.ASSISTANT, assistantText));

        return new ChatSendRes(
                new ChatMessageRes(userMsg),
                new ChatMessageRes(assistantMsg),
                session.getStatus(),
                menuCards);
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
        // 멘토 피드백 정정(2026-06-06): 펫 챗봇에서도 상담원 연결 허용.
        // (이전: EntryPoint.CS만 허용 → 펫에서 "상담원 연결" 텍스트 입력 시 펫이 "저한테 말씀하세요" 응답하던 문제)
        session.escalate();
        notifyAdminEscalation(session);
        return new ChatSessionRes(session);
    }

    /**
     * P-7 admin 알림 — best-effort. Feign 실패해도 main escalate transition은 그대로 유지.
     * lastUserMessage = 세션의 가장 최근 USER 메시지 1건.
     */
    private void notifyAdminEscalation(ChatSession session) {
        try {
            List<ChatMessage> history = messageRepository.findBySessionIdOrderByMessageIdAsc(session.getSessionId());
            String lastUser = history.stream()
                    .filter(m -> m.getRole() == MessageRole.USER)
                    .reduce((a, b) -> b)
                    .map(ChatMessage::getContent)
                    .orElse(null);
            Map<String, Object> req = new HashMap<>();
            req.put("userNo", session.getUserNo());
            req.put("sessionId", session.getSessionId());
            req.put("lastUserMessage", lastUser);
            adminFeignClient.escalateChatbot(req);
        } catch (Exception e) {
            log.warn("에스컬레이션 admin 알림 실패 (best-effort) — sessionId={}, cause={}",
                    session.getSessionId(), e.getMessage());
        }
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

    private ChatRecommendation callGemini(ChatSession session, String callerRole, String userContent) {
        try {
            Pet pet = (session.getPetNo() != null)
                    ? petService.getOrCreatePet(session.getUserNo())
                    : null;
            String extraContext = Optional.ofNullable(contextProvider)
                    .map(p -> p.buildContext(pet))
                    .orElse(null);
            String systemInstruction = promptBuilder.buildSystemInstruction(
                    session.getEntryPoint(), session.getToneMode(), pet, callerRole, extraContext);
            String json = geminiClient.generateJson(
                    systemInstruction, userContent, DEFAULT_MAX_OUTPUT_TOKENS, RECOMMEND_SCHEMA);
            return objectMapper.readValue(json, ChatRecommendation.class);
        } catch (GeminiException e) {
            log.warn("Gemini 호출 실패 — sessionId={}, cause={}", session.getSessionId(), e.getMessage());
            return new ChatRecommendation(GEMINI_FALLBACK_MESSAGE, List.of());
        } catch (Exception e) {
            log.warn("Gemini 응답 파싱 실패 — sessionId={}, cause={}", session.getSessionId(), e.getMessage());
            return new ChatRecommendation(GEMINI_FALLBACK_MESSAGE, List.of());
        }
    }

    /**
     * #1+#2 메뉴 추천 카드 검색 — 키워드 1~3개로 storeMapper.searchStore 후 dedupe + 최대 MAX_CARDS개.
     * storeMapper.searchStore는 가게/메뉴/카테고리 LIKE 통합 검색이라 키워드를 그대로 전달.
     */
    private List<MenuCardDto> searchMenuCards(List<String> keywords) {
        List<MenuCardDto> cards = new ArrayList<>();
        java.util.Set<Long> seenStoreIds = new HashSet<>();
        for (String keyword : keywords) {
            if (cards.size() >= MAX_CARDS) break;
            if (keyword == null || keyword.isBlank()) continue;
            try {
                List<StoreGetRes> stores = storeMapper.searchStore(keyword.trim());
                int taken = 0;
                for (StoreGetRes s : stores) {
                    if (taken >= MAX_STORES_PER_KEYWORD || cards.size() >= MAX_CARDS) break;
                    if (s.getId() <= 0 || !seenStoreIds.add((long) s.getId())) continue;
                    cards.add(new MenuCardDto(s, keyword));
                    taken++;
                }
            } catch (Exception e) {
                log.warn("메뉴 카드 검색 실패 — keyword={}, cause={}", keyword, e.getMessage());
            }
        }
        return cards;
    }
}
