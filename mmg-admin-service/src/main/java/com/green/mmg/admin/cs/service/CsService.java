package com.green.mmg.admin.cs.service;

import com.green.mmg.admin.common.enums.InquiryStatus;
import com.green.mmg.admin.common.enums.InquiryUserType;
import com.green.mmg.admin.cs.dto.EscalationPayload;
import com.green.mmg.admin.cs.dto.InquiryReq;
import com.green.mmg.admin.cs.dto.InquiryRes;
import com.green.mmg.admin.cs.dto.InquirySummaryRes;
import com.green.mmg.admin.cs.entity.ChatbotInquiry;
import com.green.mmg.admin.cs.repository.ChatbotInquiryRepository;
import com.green.mmg.admin.dto.feign.NotificationCreateReq;
import com.green.mmg.admin.feign.AuthFeignClient;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsService {

    private final ChatbotInquiryRepository chatbotInquiryRepository;
    private final AdminEscalationSseService escalationSseService;
    private final AuthFeignClient authFeignClient;
    private final MainFeignClient mainFeignClient;

    // 문의 현황 카드
    public InquirySummaryRes getSummary() {
        Long total = chatbotInquiryRepository.count();
        Long resolved = chatbotInquiryRepository.countByState(InquiryStatus.RESOLVED);
        Long pending = chatbotInquiryRepository.countByState(InquiryStatus.PENDING);
        return new InquirySummaryRes(total, resolved, pending);
    }

    // 문의 목록 조회
    public List<InquiryRes> getInquiryList(InquiryUserType category, InquiryStatus state) {
        List<ChatbotInquiry> list;
        if (category != null) {
            list = chatbotInquiryRepository.findByCategory(category);
        } else if (state != null) {
            list = chatbotInquiryRepository.findByState(state);
        } else {
            list = chatbotInquiryRepository.findAll();
        }

        record UserInfo(String userId, String role) {}
        Map<Long, UserInfo> userInfoMap = list.stream()
                .map(ChatbotInquiry::getUserNo)
                .distinct()
                .collect(Collectors.toMap(
                        userNo -> userNo,
                        userNo -> {
                            try {
                                var detail = authFeignClient.getUserDetail(userNo).getResultData();
                                return new UserInfo(detail.getUserId(), detail.getRole());
                            } catch (Exception e) {
                                log.warn("유저 정보 조회 실패 userNo={}", userNo);
                                return new UserInfo("#" + userNo, null);
                            }
                        }
                ));

        return list.stream()
                .map(i -> {
                    UserInfo info = userInfoMap.getOrDefault(i.getUserNo(), new UserInfo("#" + i.getUserNo(), null));
                    return new InquiryRes(
                            i.getInquiryId(), i.getUserNo(),
                            info.userId(), info.role(),
                            i.getCategory(), i.getContent(), i.getState(),
                            i.getAnswer(), i.getAnsweredAt(), i.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    // 문의 상세 조회
    public InquiryRes getInquiryDetail(Long inquiryId) {
        ChatbotInquiry i = chatbotInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("문의를 찾을 수 없습니다."));
        String userId, userRole;
        try {
            var detail = authFeignClient.getUserDetail(i.getUserNo()).getResultData();
            userId = detail.getUserId();
            userRole = detail.getRole();
        } catch (Exception e) {
            log.warn("유저 정보 조회 실패 userNo={}", i.getUserNo());
            userId = "#" + i.getUserNo();
            userRole = null;
        }
        return new InquiryRes(i.getInquiryId(), i.getUserNo(), userId, userRole, i.getCategory(),
                i.getContent(), i.getState(), i.getAnswer(), i.getAnsweredAt(), i.getCreatedAt());
    }

    // 답변 등록
    @Transactional
    public void replyInquiry(Long inquiryId, InquiryReq req) {
        ChatbotInquiry inquiry = chatbotInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("문의를 찾을 수 없습니다."));
        inquiry.reply(req.getReply());

        try {
            mainFeignClient.createNotification(new NotificationCreateReq(
                    inquiry.getUserNo(),
                    "CS_REPLY",
                    "문의 답변이 등록되었습니다.",
                    req.getReply(),
                    null
            ));
        } catch (Exception e) {
            log.warn("고객 알림 전송 실패 inquiryId={}: {}", inquiryId, e.getMessage());
        }
    }

    @Transactional
    public void createInquiry(Long userNo, String content) {
        ChatbotInquiry inquiry = new ChatbotInquiry(
                userNo, InquiryUserType.OWNER, content);
        chatbotInquiryRepository.save(inquiry);
    }

    /**
     * P-7 main 챗봇 에스컬레이션 수신 — chatbot_inquiry INSERT (CUSTOMER, PENDING) + SSE 푸시.
     * case-#40 정정 일관: chatbot_inquiry state enum은 PENDING/PROCESSING/RESOLVED 유지.
     */
    @Transactional
    public void receiveChatbotEscalation(Long userNo, Long sessionId, String lastUserMessage) {
        String content = (lastUserMessage == null || lastUserMessage.isBlank())
                ? "[에스컬레이션] 챗봇 세션에서 상담원 연결 요청"
                : lastUserMessage;
        ChatbotInquiry inquiry = new ChatbotInquiry(userNo, InquiryUserType.CUSTOMER, content);
        chatbotInquiryRepository.save(inquiry);
        escalationSseService.broadcast(new EscalationPayload(
                inquiry.getInquiryId(), userNo, sessionId, content));
    }
}