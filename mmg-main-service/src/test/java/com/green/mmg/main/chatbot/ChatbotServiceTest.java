package com.green.mmg.main.chatbot;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.chatbot.dto.ChatMessageRes;
import com.green.mmg.main.chatbot.dto.ChatSendRes;
import com.green.mmg.main.chatbot.dto.ChatSessionRes;
import com.green.mmg.main.chatbot.entity.*;
import com.green.mmg.main.pet.PetService;
import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
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
        @DisplayName("MYPET happy — 펫 lazy 생성 + ChatSession 저장")
        void mypet_happy() {
            Pet pet = sampleLv1Pet();
            when(petService.getOrCreatePet(USER_NO)).thenReturn(pet);
            ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
            when(sessionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ChatSessionRes res = chatbotService.startSession(USER_NO, EntryPoint.MYPET, ToneMode.PLAYFUL);

            assertThat(captor.getValue().getEntryPoint()).isEqualTo(EntryPoint.MYPET);
            assertThat(captor.getValue().getStatus()).isEqualTo(SessionStatus.ACTIVE);
            assertThat(res.getEntryPoint()).isEqualTo(EntryPoint.MYPET);
            verify(petService).getOrCreatePet(USER_NO);
        }

        @Test
        @DisplayName("CS happy — pet 조회 X + petNo null")
        void cs_happy() {
            ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
            when(sessionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ChatSessionRes res = chatbotService.startSession(USER_NO, EntryPoint.CS, ToneMode.SERIOUS);

            assertThat(captor.getValue().getPetNo()).isNull();
            assertThat(res.getEntryPoint()).isEqualTo(EntryPoint.CS);
            verifyNoInteractions(petService);
        }

        @Test
        @DisplayName("null entryPoint → BAD_REQUEST")
        void null_entryPoint_throws() {
            assertThatThrownBy(() -> chatbotService.startSession(USER_NO, null, ToneMode.PLAYFUL))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(sessionRepository);
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
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any()))
                    .thenReturn("system-prompt");
            when(geminiClient.generateText(anyString(), anyString(), anyInt()))
                    .thenReturn("안녕하세요! 멍멍!");
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChatSendRes res = chatbotService.sendMessage(USER_NO, SESSION_ID, "안녕");

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
            when(promptBuilder.buildSystemInstruction(any(), any(), any(), any()))
                    .thenReturn("system-prompt");
            when(geminiClient.generateText(anyString(), anyString(), anyInt()))
                    .thenThrow(new GeminiException("503 Service Unavailable"));
            ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
            when(messageRepository.save(msgCaptor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChatSendRes res = chatbotService.sendMessage(USER_NO, SESSION_ID, "테스트");

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

            ChatSendRes res = chatbotService.sendMessage(USER_NO, SESSION_ID, "여보세요");

            assertThat(res.getAssistantMessage().getContent()).contains("상담원 연결 대기");
            verifyNoInteractions(geminiClient);
        }

        @Test
        @DisplayName("blank content → BAD_REQUEST")
        void blank_throws() {
            assertThatThrownBy(() -> chatbotService.sendMessage(USER_NO, SESSION_ID, "   "))
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

            assertThatThrownBy(() -> chatbotService.sendMessage(USER_NO, SESSION_ID, "hi"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("다른 사용자 세션 → FORBIDDEN")
        void otherUser_forbidden() {
            ChatSession session = activeMypet();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> chatbotService.sendMessage(OTHER_USER, SESSION_ID, "hi"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("세션 없음 → NOT_FOUND")
        void notFound() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatbotService.sendMessage(USER_NO, SESSION_ID, "hi"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
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
        @DisplayName("CS escalate happy → status=ESCALATED")
        void cs_escalate_happy() {
            ChatSession session = new ChatSession(USER_NO, null, EntryPoint.CS, ToneMode.SERIOUS);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            ChatSessionRes res = chatbotService.escalate(USER_NO, SESSION_ID);

            assertThat(res.getStatus()).isEqualTo(SessionStatus.ESCALATED);
            assertThat(session.getEscalatedAt()).isNotNull();
        }

        @Test
        @DisplayName("MYPET escalate → BAD_REQUEST (CS만 가능)")
        void mypet_escalate_blocked() {
            ChatSession session = new ChatSession(USER_NO, PET_NO, EntryPoint.MYPET, ToneMode.PLAYFUL);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> chatbotService.escalate(USER_NO, SESSION_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
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
