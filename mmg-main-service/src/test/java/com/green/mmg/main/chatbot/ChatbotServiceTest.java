package com.green.mmg.main.chatbot;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.chatbot.dto.ChatMessageRes;
import com.green.mmg.main.chatbot.dto.ChatSendRes;
import com.green.mmg.main.chatbot.dto.ChatSessionRes;
import com.green.mmg.main.chatbot.dto.MenuCardDto;
import com.green.mmg.main.chatbot.entity.*;
import com.green.mmg.main.feign.AdminFeignClient;
import com.green.mmg.main.pet.PetService;
import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
import com.green.mmg.main.store.StoreMapper;
import com.green.mmg.main.store.model.StoreGetRes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatbotService — 단위 테스트")
class ChatbotServiceTest {

    @Mock private ChatSessionRepository sessionRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private PetService petService;
    @Mock private GeminiClient geminiClient;
    @Mock private ChatbotPromptBuilder promptBuilder;
    @Mock private AdminFeignClient adminFeignClient;
    @Mock private StoreMapper storeMapper;

    @InjectMocks
    private ChatbotService chatbotService;

    private static final Long USER_NO = 42L;
    private static final Long OTHER_USER = 99L;
    private static final Long SESSION_ID = 7L;
    private static final Long PET_NO = 11L;

    private Pet sampleLv1Pet() {
        return new Pet(USER_NO, PetSpecies.DOG, "멍멍이");
    }

    @Nested
    @DisplayName("startSession")
    class Start {

        @Test
        @DisplayName("MYPET happy (CUSTOMER) — 펫 lazy 생성 + ChatSession 저장")
        void mypet_happy() {
            Pet pet = sampleLv1Pet();
            when(petService.getOrCreatePet(USER_NO)).thenReturn(pet);
            ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
            when(sessionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ChatSessionRes res = chatbotService.startSession(USER_NO, "CUSTOMER", EntryPoint.MYPET, ToneMode.PLAYFUL);

            assertThat(captor.getValue().getEntryPoint()).isEqualTo(EntryPoint.MYPET);
            assertThat(captor.getValue().getStatus()).isEqualTo(SessionStatus.ACTIVE);
            assertThat(res.getEntryPoint()).isEqualTo(EntryPoint.MYPET);
            verify(petService).getOrCreatePet(USER_NO);
        }

        @Test
        @DisplayName("CS happy (CUSTOMER) — pet 조회 X + petNo null")
        void cs_happy() {
            ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
            when(sessionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ChatSessionRes res = chatbotService.startSession(USER_NO, "CUSTOMER", EntryPoint.CS, ToneMode.SERIOUS);

            assertThat(captor.getValue().getPetNo()).isNull();
            assertThat(res.getEntryPoint()).isEqualTo(EntryPoint.CS);
            verifyNoInteractions(petService);
        }

        @Test
        @DisplayName("null entryPoint → BAD_REQUEST")
        void null_entryPoint_throws() {
            assertThatThrownBy(() -> chatbotService.startSession(USER_NO, "CUSTOMER", null, ToneMode.PLAYFUL))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(sessionRepository);
        }

        // 자잘 에러 트랙(2026-05-23) — MYPET role 가드 (펫 자동 생성 side effect 차단)

        @Test
        @DisplayName("MYPET + OWNER → FORBIDDEN + pet 생성 skip + session 저장 X")
        void mypet_owner_forbidden() {
            assertThatThrownBy(() -> chatbotService.startSession(USER_NO, "OWNER", EntryPoint.MYPET, ToneMode.PLAYFUL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("펫 챗봇은 고객 전용")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
            verifyNoInteractions(petService);
            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("MYPET + RIDER → FORBIDDEN + pet 생성 skip + session 저장 X")
        void mypet_rider_forbidden() {
            assertThatThrownBy(() -> chatbotService.startSession(USER_NO, "RIDER", EntryPoint.MYPET, ToneMode.PLAYFUL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("펫 챗봇은 고객 전용")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
            verifyNoInteractions(petService);
            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("CS + OWNER happy — pet 조회 X + petNo null (사장도 CS 사용 가능)")
        void cs_owner_happy() {
            ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
            when(sessionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ChatSessionRes res = chatbotService.startSession(USER_NO, "OWNER", EntryPoint.CS, ToneMode.SERIOUS);

            assertThat(captor.getValue().getPetNo()).isNull();
            assertThat(res.getEntryPoint()).isEqualTo(EntryPoint.CS);
            verifyNoInteractions(petService);
        }

        @Test
        @DisplayName("CS + RIDER happy — pet 조회 X + petNo null (라이더도 CS 사용 가능)")
        void cs_rider_happy() {
            ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
            when(sessionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ChatSessionRes res = chatbotService.startSession(USER_NO, "RIDER", EntryPoint.CS, ToneMode.SERIOUS);

            assertThat(captor.getValue().getPetNo()).isNull();
            assertThat(res.getEntryPoint()).isEqualTo(EntryPoint.CS);
            verifyNoInteractions(petService);
        }
    }

    @Nested
    @DisplayName("sendMessage")
    class Send {

        private ChatSession activeMypet() {
            ChatSession s = new ChatSession(USER_NO, PET_NO, EntryPoint.MYPET, ToneMode.PLAYFUL);
            return s;
        }

        @Test
        @DisplayName("happy — USER 저장 + Gemini 호출 + ASSISTANT 저장 + 응답 DTO")
        void happy_savesBothMessages() {
            ChatSession session = activeMypet();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(petService.getOrCreatePet(USER_NO)).thenReturn(sampleLv1Pet());
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any(), any()))
                    .thenReturn("system-prompt");
            when(geminiClient.generateJson(anyString(), anyString(), anyInt(), anyString()))
                    .thenReturn("{\"message\":\"안녕하세요! 멍멍!\",\"menuKeywords\":[]}");
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChatSendRes res = chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "안녕");

            assertThat(res.getUserMessage().getRole()).isEqualTo(MessageRole.USER);
            assertThat(res.getUserMessage().getContent()).isEqualTo("안녕");
            assertThat(res.getAssistantMessage().getRole()).isEqualTo(MessageRole.ASSISTANT);
            assertThat(res.getAssistantMessage().getContent()).isEqualTo("안녕하세요! 멍멍!");
            verify(messageRepository, times(2)).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Gemini 실패 → fallback 메시지 저장")
        void gemini_failure_fallback() {
            ChatSession session = activeMypet();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(petService.getOrCreatePet(USER_NO)).thenReturn(sampleLv1Pet());
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any(), any()))
                    .thenReturn("system-prompt");
            when(geminiClient.generateJson(anyString(), anyString(), anyInt(), anyString()))
                    .thenThrow(new GeminiException("503 Service Unavailable"));
            // searchMenuCards 폴백 시 호출 안 함
            ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
            when(messageRepository.save(msgCaptor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChatSendRes res = chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "테스트");

            List<ChatMessage> saved = msgCaptor.getAllValues();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
            assertThat(saved.get(1).getContent()).contains("답변을 드리기 어려워요");
            assertThat(res.getAssistantMessage().getContent()).contains("답변을 드리기 어려워요");
        }

        @Test
        @DisplayName("ESCALATED 상태 → Gemini 호출 X + 안내 메시지")
        void escalated_skipsGemini() {
            ChatSession session = new ChatSession(USER_NO, null, EntryPoint.CS, ToneMode.SERIOUS);
            session.escalate();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChatSendRes res = chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "여보세요");

            assertThat(res.getAssistantMessage().getContent()).contains("상담원 연결 대기");
            verifyNoInteractions(geminiClient);
        }

        @Test
        @DisplayName("blank content → BAD_REQUEST")
        void blank_throws() {
            assertThatThrownBy(() -> chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "   "))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("CLOSED 세션 → BAD_REQUEST")
        void closed_throws() {
            ChatSession session = activeMypet();
            session.close();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "hi"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("다른 사용자 세션 → FORBIDDEN")
        void otherUser_forbidden() {
            ChatSession session = activeMypet();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> chatbotService.sendMessage(OTHER_USER, "CUSTOMER", SESSION_ID, "hi"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("세션 없음 → NOT_FOUND")
        void notFound() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "hi"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("OWNER callerRole → promptBuilder에 OWNER 그대로 전파")
        void owner_role_propagated_to_prompt() {
            ChatSession session = new ChatSession(USER_NO, null, EntryPoint.CS, ToneMode.SERIOUS);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any(), any()))
                    .thenReturn("sys");
            when(geminiClient.generateJson(anyString(), anyString(), anyInt(), anyString()))
                    .thenReturn("{\"message\":\"정산은...\",\"menuKeywords\":[]}");
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            chatbotService.sendMessage(USER_NO, "OWNER", SESSION_ID, "정산 어디서 봐요?");

            ArgumentCaptor<String> roleCap = ArgumentCaptor.forClass(String.class);
            verify(promptBuilder).buildSystemInstruction(any(), any(), any(), roleCap.capture(), any());
            assertThat(roleCap.getValue()).isEqualTo("OWNER");
        }

        // ── #1+#2 메뉴 추천 카드 (2026-06-06) ────────────────────────

        @Test
        @DisplayName("#1+#2 menuKeywords 있으면 storeMapper.searchStore로 카드 검색 + ChatSendRes.menuCards에 첨부")
        void menuKeywords_triggerStoreSearch_andAttachCards() {
            ChatSession session = activeMypet();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(petService.getOrCreatePet(USER_NO)).thenReturn(sampleLv1Pet());
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any(), any()))
                    .thenReturn("sys");
            when(geminiClient.generateJson(anyString(), anyString(), anyInt(), anyString()))
                    .thenReturn("{\"message\":\"치킨 어때요?\",\"menuKeywords\":[\"치킨\",\"피자\"]}");
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            StoreGetRes s1 = new StoreGetRes(); s1.setId(101); s1.setName("황금올리브치킨"); s1.setPic("/u/c1.jpg");
            StoreGetRes s2 = new StoreGetRes(); s2.setId(102); s2.setName("BHC 강남점"); s2.setPic("/u/c2.jpg");
            StoreGetRes s3 = new StoreGetRes(); s3.setId(201); s3.setName("도미노 강남"); s3.setPic("/u/p1.jpg");
            when(storeMapper.searchStore("치킨")).thenReturn(java.util.List.of(s1, s2));
            when(storeMapper.searchStore("피자")).thenReturn(java.util.List.of(s3));

            ChatSendRes res = chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "야식 추천");

            assertThat(res.getMenuCards()).hasSize(3);
            assertThat(res.getMenuCards()).extracting(MenuCardDto::getStoreId)
                    .containsExactly(101L, 102L, 201L);
            assertThat(res.getMenuCards().get(0).getMatchedKeyword()).isEqualTo("치킨");
            assertThat(res.getMenuCards().get(2).getMatchedKeyword()).isEqualTo("피자");
            verify(storeMapper).searchStore("치킨");
            verify(storeMapper).searchStore("피자");
        }

        @Test
        @DisplayName("#1+#2 menuKeywords 비어있으면 storeMapper 호출 X + menuCards 빈 리스트")
        void empty_menuKeywords_skipsStoreSearch() {
            ChatSession session = new ChatSession(USER_NO, null, EntryPoint.CS, ToneMode.SERIOUS);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any(), any())).thenReturn("sys");
            when(geminiClient.generateJson(anyString(), anyString(), anyInt(), anyString()))
                    .thenReturn("{\"message\":\"환불은 마이페이지에서...\",\"menuKeywords\":[]}");
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChatSendRes res = chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "환불 어디서 해요?");

            assertThat(res.getMenuCards()).isEmpty();
            verifyNoInteractions(storeMapper);
        }

        @Test
        @DisplayName("#1+#2 같은 가게가 여러 키워드에 매칭되면 dedupe + 키워드당 2개 제한 (2026-06-06 정책 변경)")
        void dedupe_andPerKeywordLimit() {
            ChatSession session = activeMypet();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(petService.getOrCreatePet(USER_NO)).thenReturn(sampleLv1Pet());
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any(), any())).thenReturn("sys");
            when(geminiClient.generateJson(anyString(), anyString(), anyInt(), anyString()))
                    .thenReturn("{\"message\":\"오케이\",\"menuKeywords\":[\"치킨\",\"양념\"]}");
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            StoreGetRes a = new StoreGetRes(); a.setId(1); a.setName("A");
            StoreGetRes b = new StoreGetRes(); b.setId(2); b.setName("B");
            StoreGetRes c = new StoreGetRes(); c.setId(3); c.setName("C");
            StoreGetRes d = new StoreGetRes(); d.setId(4); d.setName("D");
            when(storeMapper.searchStore("치킨")).thenReturn(java.util.List.of(a, b, c, d)); // 4건이지만 2개만 채택 (MAX_STORES_PER_KEYWORD=2)
            when(storeMapper.searchStore("양념")).thenReturn(java.util.List.of(a, b)); // 모두 중복 → 0건

            ChatSendRes res = chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "추천");

            assertThat(res.getMenuCards()).extracting(MenuCardDto::getStoreId)
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("[강화] menuKeywords 5개 → 최대 8 카드 (키워드당 2개씩 분산)")
        void five_keywords_eight_cards_max() {
            ChatSession session = activeMypet();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(petService.getOrCreatePet(USER_NO)).thenReturn(sampleLv1Pet());
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any(), any())).thenReturn("sys");
            when(geminiClient.generateJson(anyString(), anyString(), anyInt(), anyString()))
                    .thenReturn("{\"message\":\"메뉴 5종 추천\",\"menuKeywords\":[\"치킨\",\"피자\",\"파스타\",\"초밥\",\"떡볶이\"]}");
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            StoreGetRes s1 = new StoreGetRes(); s1.setId(1); s1.setName("치킨1");
            StoreGetRes s2 = new StoreGetRes(); s2.setId(2); s2.setName("치킨2");
            StoreGetRes s3 = new StoreGetRes(); s3.setId(3); s3.setName("피자1");
            StoreGetRes s4 = new StoreGetRes(); s4.setId(4); s4.setName("피자2");
            StoreGetRes s5 = new StoreGetRes(); s5.setId(5); s5.setName("파스타1");
            StoreGetRes s6 = new StoreGetRes(); s6.setId(6); s6.setName("파스타2");
            StoreGetRes s7 = new StoreGetRes(); s7.setId(7); s7.setName("초밥1");
            StoreGetRes s8 = new StoreGetRes(); s8.setId(8); s8.setName("초밥2");
            when(storeMapper.searchStore("치킨")).thenReturn(java.util.List.of(s1, s2));
            when(storeMapper.searchStore("피자")).thenReturn(java.util.List.of(s3, s4));
            when(storeMapper.searchStore("파스타")).thenReturn(java.util.List.of(s5, s6));
            when(storeMapper.searchStore("초밥")).thenReturn(java.util.List.of(s7, s8));
            // 5번째 키워드(떡볶이)는 MAX_CARDS=8 도달로 호출 X — stub 생략

            ChatSendRes res = chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "추천 5개");

            assertThat(res.getMenuCards()).hasSize(8); // MAX_CARDS=8 상한
            assertThat(res.getMenuCards()).extracting(MenuCardDto::getStoreId)
                    .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
            verify(storeMapper, never()).searchStore("떡볶이");
        }

        @Test
        @DisplayName("#1+#2 Gemini 응답 JSON 파싱 실패 → fallback 메시지 + 빈 카드")
        void invalid_json_fallback() {
            ChatSession session = activeMypet();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(petService.getOrCreatePet(USER_NO)).thenReturn(sampleLv1Pet());
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any(), any())).thenReturn("sys");
            when(geminiClient.generateJson(anyString(), anyString(), anyInt(), anyString()))
                    .thenReturn("이건 JSON이 아님");
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChatSendRes res = chatbotService.sendMessage(USER_NO, "CUSTOMER", SESSION_ID, "테스트");

            assertThat(res.getAssistantMessage().getContent()).contains("답변을 드리기 어려워요");
            assertThat(res.getMenuCards()).isEmpty();
            verifyNoInteractions(storeMapper);
        }
    }

    @Nested
    @DisplayName("listMessages")
    class List_ {

        @Test
        @DisplayName("권한 일치 + 메시지 DTO 리스트 반환")
        void happy() {
            ChatSession session = new ChatSession(USER_NO, PET_NO, EntryPoint.MYPET, ToneMode.PLAYFUL);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            ChatMessage m1 = new ChatMessage(SESSION_ID, MessageRole.USER, "hi");
            ChatMessage m2 = new ChatMessage(SESSION_ID, MessageRole.ASSISTANT, "hello");
            when(messageRepository.findBySessionIdOrderByMessageIdAsc(SESSION_ID))
                    .thenReturn(java.util.List.of(m1, m2));

            java.util.List<ChatMessageRes> result = chatbotService.listMessages(USER_NO, SESSION_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRole()).isEqualTo(MessageRole.USER);
            assertThat(result.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        }

        @Test
        @DisplayName("다른 사용자 세션 → FORBIDDEN")
        void otherUser_forbidden() {
            ChatSession session = new ChatSession(USER_NO, PET_NO, EntryPoint.MYPET, ToneMode.PLAYFUL);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> chatbotService.listMessages(OTHER_USER, SESSION_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("escalate / close")
    class Transition {

        @Test
        @DisplayName("CS escalate happy → status=ESCALATED + admin Feign 1회 호출")
        void cs_escalate_happy() {
            ChatSession session = new ChatSession(USER_NO, null, EntryPoint.CS, ToneMode.SERIOUS);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            ChatMessage userMsg = new ChatMessage(SESSION_ID, MessageRole.USER, "도와주세요");
            when(messageRepository.findBySessionIdOrderByMessageIdAsc(any()))
                    .thenReturn(java.util.List.of(userMsg));
            when(adminFeignClient.escalateChatbot(any())).thenReturn(new ResultResponse<>("ok", null));

            ChatSessionRes res = chatbotService.escalate(USER_NO, SESSION_ID);

            assertThat(res.getStatus()).isEqualTo(SessionStatus.ESCALATED);
            assertThat(session.getEscalatedAt()).isNotNull();
            verify(adminFeignClient, times(1)).escalateChatbot(any());
        }

        @Test
        @DisplayName("admin Feign 실패 → escalate state는 그대로 ESCALATED 유지 (best-effort)")
        void cs_escalate_feignFails_stateStillEscalated() {
            ChatSession session = new ChatSession(USER_NO, null, EntryPoint.CS, ToneMode.SERIOUS);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(messageRepository.findBySessionIdOrderByMessageIdAsc(any()))
                    .thenReturn(java.util.List.of());
            when(adminFeignClient.escalateChatbot(any()))
                    .thenThrow(new RuntimeException("admin down"));

            ChatSessionRes res = chatbotService.escalate(USER_NO, SESSION_ID);

            assertThat(res.getStatus()).isEqualTo(SessionStatus.ESCALATED);
        }

        @Test
        @DisplayName("MYPET escalate happy (2026-06-06 정정) → status=ESCALATED + admin Feign 호출")
        void mypet_escalate_now_allowed() {
            ChatSession session = new ChatSession(USER_NO, PET_NO, EntryPoint.MYPET, ToneMode.PLAYFUL);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(messageRepository.findBySessionIdOrderByMessageIdAsc(any()))
                    .thenReturn(java.util.List.of());
            when(adminFeignClient.escalateChatbot(any())).thenReturn(new ResultResponse<>("ok", null));

            ChatSessionRes res = chatbotService.escalate(USER_NO, SESSION_ID);

            assertThat(res.getStatus()).isEqualTo(SessionStatus.ESCALATED);
            assertThat(session.getEscalatedAt()).isNotNull();
            verify(adminFeignClient, times(1)).escalateChatbot(any());
        }

        @Test
        @DisplayName("close happy → status=CLOSED + closedAt")
        void close_happy() {
            ChatSession session = new ChatSession(USER_NO, null, EntryPoint.CS, ToneMode.SERIOUS);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            ChatSessionRes res = chatbotService.closeSession(USER_NO, SESSION_ID);

            assertThat(res.getStatus()).isEqualTo(SessionStatus.CLOSED);
            assertThat(session.getClosedAt()).isNotNull();
        }
    }
}
