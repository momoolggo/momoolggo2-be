package com.green.mmg.admin.cs.service;

import com.green.mmg.admin.common.enums.InquiryStatus;
import com.green.mmg.admin.common.enums.InquiryUserType;
import com.green.mmg.admin.cs.dto.InquiryReq;
import com.green.mmg.admin.cs.dto.InquirySummaryRes;
import com.green.mmg.admin.cs.entity.ChatbotInquiry;
import com.green.mmg.admin.cs.repository.ChatbotInquiryRepository;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CsService {

    private final ChatbotInquiryRepository chatbotInquiryRepository;

    // 문의 현황 카드
    public InquirySummaryRes getSummary() {
        Long total = chatbotInquiryRepository.count();
        Long resolved = chatbotInquiryRepository.countByState(InquiryStatus.RESOLVED);
        Long pending = chatbotInquiryRepository.countByState(InquiryStatus.PENDING);
        return new InquirySummaryRes(total, resolved, pending);
    }

    // 문의 목록 조회
    public List<ChatbotInquiry> getInquiryList(
            InquiryUserType category, InquiryStatus state) {
        if (category != null) {
            return chatbotInquiryRepository.findByCategory(category);
        }
        if (state != null) {
            return chatbotInquiryRepository.findByState(state);
        }
        return chatbotInquiryRepository.findAll();
    }

    // 문의 상세 조회
    public ChatbotInquiry getInquiryDetail(Long inquiryId) {
        return chatbotInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("문의를 찾을 수 없습니다."));
    }

    // 답변 등록
    @Transactional
    public void replyInquiry(Long inquiryId, InquiryReq req) {
        ChatbotInquiry inquiry = chatbotInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("문의를 찾을 수 없습니다."));
        inquiry.reply(req.getReply());
    }

    @Transactional
    public void createInquiry(Long userNo, String content) {
        ChatbotInquiry inquiry = new ChatbotInquiry(
                userNo, InquiryUserType.OWNER, content);
        chatbotInquiryRepository.save(inquiry);
    }
}