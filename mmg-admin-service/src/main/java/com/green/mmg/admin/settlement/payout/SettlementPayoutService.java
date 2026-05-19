package com.green.mmg.admin.settlement.payout;

import com.green.mmg.admin.settlement.entity.Settlement;
import com.green.mmg.admin.settlement.repository.SettlementRepository;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 정산 지급 서비스
 * 관리자가 정산 DONE 확정 시 토스페이먼츠 지급대행 API 호출
 *
 * [실제 운영 시 필요한 것]
 * 1. 사업자 등록번호
 * 2. 토스페이먼츠 지급대행 서비스 계약
 * 3. application.yml에 toss.payout.secret-key 설정
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementPayoutService {

    private final SettlementRepository settlementRepository;
    private final TossPayoutClient tossPayoutClient;

    /**
     * 정산 지급 실행
     * completeSettlement() 호출 시 자동으로 지급 요청
     */
    @Transactional
    public void executePayout(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));

        if (settlement.getBankAccount() == null) {
            log.warn("계좌 정보 없음 settlementId={}", settlementId);
            return;
        }

        // 계좌 파싱 (예: "국민은행 111-2222-3333" → bankCode + accountNumber)
        BankInfo bankInfo = parseBankAccount(settlement.getBankAccount());

        String requestedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

        try {
            TossPayoutClient.TossPayoutReq req = new TossPayoutClient.TossPayoutReq(
                    settlement.getNetAmount(),
                    bankInfo.bankCode(),
                    bankInfo.accountNumber(),
                    bankInfo.holderName(),
                    requestedAt
            );

            TossPayoutClient.TossPayoutRes res = tossPayoutClient.requestPayout(req);
            log.info("토스 지급 요청 완료 settlementId={} payoutId={} status={}",
                    settlementId, res.payoutId(), res.status());

            // 지급 ID 저장 (추후 상태 조회용)
            settlement.setTossPayoutId(res.payoutId());

        } catch (Exception e) {
            log.error("토스 지급 요청 실패 settlementId={} error={}", settlementId, e.getMessage());
            // 실패해도 정산 DONE 처리는 유지 (수동 재처리 가능하도록)
        }
    }

    /**
     * 은행명 → 토스 은행 코드 변환
     * 실제 운영 시 DB나 enum으로 관리
     */
    private BankInfo parseBankAccount(String bankAccount) {
        // 예: "국민은행 111-2222-3333"
        String[] parts = bankAccount.split(" ", 2);
        String bankName = parts.length > 0 ? parts[0] : "";
        String accountNo = parts.length > 1 ? parts[1].replace("-", "") : "";

        String bankCode = switch (bankName) {
            case "국민은행", "국민" -> "004";
            case "신한은행", "신한" -> "088";
            case "하나은행", "하나" -> "081";
            case "우리은행", "우리" -> "020";
            case "농협은행", "농협" -> "011";
            case "카카오뱅크", "카카오" -> "090";
            case "토스뱅크", "토스" -> "092";
            default -> "004"; // 기본값: 국민은행
        };

        return new BankInfo(bankCode, accountNo, "예금주");
    }

    private record BankInfo(String bankCode, String accountNumber, String holderName) {}
}
