package com.green.mmg.admin.blind.service;

import com.green.mmg.admin.blind.dto.BlindReq;
import com.green.mmg.admin.blind.entity.Blind;
import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindStatus;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlindService {

    private final BlindRepository blindRepository;

    // 블라인드 목록 조회
    public List<Blind> getBlindList(BlindStatus status) {
        if (status != null) {
            return blindRepository.findByStatus(status);
        }
        return blindRepository.findAll();
    }

    // 블라인드 상세 조회
    public Blind getBlindDetail(Long blindId) {
        return blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
    }

    // 블라인드 처리
    @Transactional
    public void blindReview(BlindReq req) {
        Blind blind = new Blind(req.getReviewNo(), req.getUserNo(), req.getReason());
        blindRepository.save(blind);
        // TODO: 알림 발송 (Redis Pub/Sub - Phase 4)
    }

    // 블라인드 확정 (REVIEWING → BLINDED)
    @Transactional
    public void confirmBlind(Long blindId) {
        Blind blind = blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
        blind.confirm();
        // TODO: 고객에게 소명 알림 발송 (Phase 4 - Redis Pub/Sub)
    }

    // 블라인드 해제 (리뷰 수정 시 자동)
    @Transactional
    public void releaseBlind(Long blindId) {
        Blind blind = blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
        blind.release();
    }

    // 계정 정지
    @Transactional
    public void suspendUser(Long blindId) {
        Blind blind = blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
        blind.suspend();
    }

    // 영구 정지
    @Transactional
    public void permanentSuspend(Long blindId) {
        Blind blind = blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
        blind.permanentSuspend();
    }

    // 소명 기간 초과 자동 계정 정지 (스케줄러용)
    @Transactional
    public void autoSuspendExpired() {
        List<Blind> expiredList = blindRepository
                .findByStatusAndEndsAtBefore(BlindStatus.BLINDED, LocalDateTime.now());
        expiredList.forEach(Blind::suspend);
    }
}