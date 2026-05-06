package com.green.mmg.admin.cs.repository;

import com.green.mmg.admin.common.enums.InquiryStatus;
import com.green.mmg.admin.common.enums.InquiryUserType;
import com.green.mmg.admin.cs.entity.ChatbotInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotInquiryRepository extends JpaRepository<ChatbotInquiry, Long> {

    // 상태별 조회
    List<ChatbotInquiry> findByState(InquiryStatus state);

    // 카테고리별 조회
    List<ChatbotInquiry> findByCategory(InquiryUserType category);

    // 상태별 건수
    Long countByState(InquiryStatus state);
}