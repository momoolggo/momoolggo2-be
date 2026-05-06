package com.green.mmg.admin.cs.service;

import com.green.mmg.admin.common.enums.FaqCategory;
import com.green.mmg.admin.cs.dto.FaqReq;
import com.green.mmg.admin.cs.entity.Faq;
import com.green.mmg.admin.cs.repository.FaqRepository;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    // FAQ 목록 조회 (카테고리 필터)
    public List<Faq> getFaqList(FaqCategory type) {
        if (type != null) {
            return faqRepository.findByType(type);
        }
        return faqRepository.findAll();
    }

    // FAQ 등록
    @Transactional
    public void createFaq(FaqReq req) {
        faqRepository.save(new Faq(req));
    }

    // FAQ 수정
    @Transactional
    public void updateFaq(Long faqId, FaqReq req) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ를 찾을 수 없습니다."));
        faq.update(req);
    }

    // FAQ 삭제
    @Transactional
    public void deleteFaq(Long faqId) {
        faqRepository.findById(faqId)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ를 찾을 수 없습니다."));
        faqRepository.deleteById(faqId);
    }

    // 노출여부 토글
    @Transactional
    public void toggleVisible(Long faqId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ를 찾을 수 없습니다."));
        faq.toggleVisible();
    }
}