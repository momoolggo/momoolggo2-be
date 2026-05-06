package com.green.mmg.admin.dashboard.service;

import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindStatus;
import com.green.mmg.admin.common.enums.InquiryStatus;
import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.cs.repository.ChatbotInquiryRepository;
import com.green.mmg.admin.dashboard.dto.DashboardRes;
import com.green.mmg.admin.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BlindRepository blindRepository;
    private final ChatbotInquiryRepository chatbotInquiryRepository;
    private final SettlementRepository settlementRepository;

    public DashboardRes getDashboard() {
        // 블라인드 건수 (검토중 + 확정)
        Long totalBlinds = blindRepository.countByStatus(BlindStatus.REVIEWING)
                + blindRepository.countByStatus(BlindStatus.BLINDED);

        // 미처리 문의 수
        Long pendingInquiries = chatbotInquiryRepository
                .countByState(InquiryStatus.PENDING);

        // 정산 대기 건수
        Long pendingSettlements = settlementRepository
                .countByStatus(SettlementsStatus.PENDING);

        return new DashboardRes(totalBlinds, pendingInquiries, pendingSettlements);
    }
}