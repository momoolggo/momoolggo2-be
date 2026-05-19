package com.green.mmg.admin.blind.service;

import com.green.mmg.admin.blind.dto.BlindReq;
import com.green.mmg.admin.blind.dto.BlindRes;
import com.green.mmg.admin.blind.entity.Blind;
import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindStatus;
import com.green.mmg.admin.dto.feign.UserSuspensionReq;
import com.green.mmg.admin.feign.AuthFeignClient;
import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlindService {

    private final BlindRepository blindRepository;

    @Autowired(required = false)
    private AuthFeignClient authFeignClient;

    // 블라인드 목록 조회
    public List<BlindRes> getBlindList(BlindStatus status) {
        List<Blind> blinds = status != null
                ? blindRepository.findByStatus(status)
                : blindRepository.findAll();

        List<Long> userNos = blinds.stream()
                .map(Blind::getUserNo)
                .distinct()
                .collect(Collectors.toList());

        // AuthFeignClient 없거나 목록 비어있으면 빈 맵
        Map<Long, UserBriefDto> userMap = Collections.emptyMap();
        try {
            if (authFeignClient != null && !userNos.isEmpty()) {
                userMap = authFeignClient.getUsers(userNos)
                        .stream()
                        .collect(Collectors.toMap(UserBriefDto::getUserNo, u -> u));
            }
        } catch (Exception e) {
            log.warn("auth-service 사용자 조회 실패 - 기본값 사용: {}", e.getMessage());
        }

        final Map<Long, UserBriefDto> finalUserMap = userMap;

        return blinds.stream().map(b -> {
            UserBriefDto user = finalUserMap.get(b.getUserNo());
            return new BlindRes(
                    b.getBlindId(), b.getReviewNo(), b.getUserNo(),
                    b.getReason(), b.getDurationDays(), b.getStatus(),
                    b.getStartAt(), b.getEndsAt(), b.getCreatedAt(),
                    b.getStoreName(), b.getContent(), b.getRating(), b.getWriter(),
                    user != null ? user.getName() : null,
                    user != null ? user.getTel() : null
            );
        }).collect(Collectors.toList());
    }

    // 블라인드 상세 조회
    public Blind getBlindDetail(Long blindId) {
        return blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
    }

    // 블라인드 처리
    @Transactional
    public void blindReview(BlindReq req) {
        Blind blind = new Blind(
                req.getReviewNo(), req.getUserNo(), req.getReason(),
                req.getStoreName(), req.getContent(), req.getRating(), req.getWriter()
        );
        blindRepository.save(blind);
    }

    // 블라인드 확정 (REVIEWING → BLINDED)
    @Transactional
    public void confirmBlind(Long blindId) {
        Blind blind = blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
        blind.confirm();
        // TODO: 고객에게 소명 알림 발송 (Phase 4 - Redis Pub/Sub)
    }

    // 블라인드 해제
    @Transactional
    public void releaseBlind(Long blindId) {
        Blind blind = blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
        blind.release();
    }

    // 계정 정지 (blind + auth-service 회원 상태 동기화)
    @Transactional
    public void suspendUser(Long blindId) {
        Blind blind = blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
        blind.suspend();
        // auth-service 회원 상태 계정정지로 변경
        try {
            if (authFeignClient != null && blind.getUserNo() != null) {
                authFeignClient.suspendUser(blind.getUserNo(),
                        new UserSuspensionReq(15, "리뷰 블라인드 소명 미이행"));
            }
        } catch (Exception e) {
            log.warn("auth-service 계정정지 동기화 실패 userNo={} error={}", blind.getUserNo(), e.getMessage());
        }
    }

    // 영구 정지 (blind + auth-service 회원 상태 동기화)
    @Transactional
    public void permanentSuspend(Long blindId) {
        Blind blind = blindRepository.findById(blindId)
                .orElseThrow(() -> new ResourceNotFoundException("블라인드 정보를 찾을 수 없습니다."));
        blind.permanentSuspend();
        // auth-service 회원 상태 영구정지로 변경
        try {
            if (authFeignClient != null && blind.getUserNo() != null) {
                authFeignClient.suspendUser(blind.getUserNo(),
                        new UserSuspensionReq(0, "반복 위반으로 인한 영구정지")); // 0 = 영구
            }
        } catch (Exception e) {
            log.warn("auth-service 영구정지 동기화 실패 userNo={} error={}", blind.getUserNo(), e.getMessage());
        }
    }

    // 소명 기간 초과 자동 계정 정지 (스케줄러용)
    @Transactional
    public void autoSuspendExpired() {
        List<Blind> expiredList = blindRepository
                .findByStatusAndEndsAtBefore(BlindStatus.BLINDED, LocalDateTime.now());
        expiredList.forEach(Blind::suspend);
    }


}