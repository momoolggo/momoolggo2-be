package com.green.mmg.admin.dashboard.service;

import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindStatus;
import com.green.mmg.admin.common.enums.InquiryStatus;
import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.cs.repository.ChatbotInquiryRepository;
import com.green.mmg.admin.dashboard.dto.DashboardRes;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.admin.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MainFeignClient mainFeignClient;

    public DashboardRes getDashboard() {
        var stats = mainFeignClient.getTodayStats().getResultData();
        return new DashboardRes(
                stats.getNewUserCount(),
                stats.getStoreCount(),
                stats.getReviewCount()
        );
    }
}