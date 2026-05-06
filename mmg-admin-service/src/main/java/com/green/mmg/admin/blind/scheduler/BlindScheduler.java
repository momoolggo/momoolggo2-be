package com.green.mmg.admin.blind.scheduler;

import com.green.mmg.admin.blind.entity.Blind;
import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindScheduler {

    private final BlindRepository blindRepository;

    // 매일 자정 실행
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoSuspendExpired() {
        log.info("블라인드 소명 기간 체크 스케줄러 실행: {}", LocalDateTime.now());

        // 소명 기간 지난 블라인드 목록 조회
        List<Blind> expiredList = blindRepository
                .findByStatusAndEndsAtBefore(BlindStatus.BLINDED, LocalDateTime.now());

        for (Blind blind : expiredList) {
            Long blindCount = blindRepository.countByUserNo(blind.getUserNo());

            if (blindCount >= 3) {
                // 3회 이상 → 영구 정지
                blind.permanentSuspend();
                log.info("영구 정지 - userNo: {}", blind.getUserNo());

            } else {
                // 1~2회 → 15일 정지
                blind.suspend();
                log.info("계정 15일 정지 - userNo: {}", blind.getUserNo());
                // TODO: 알림 발송 (Phase 4)
            }
        }
    }
}