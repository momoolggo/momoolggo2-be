package com.green.mmg.admin.penalty.service;

import com.green.mmg.admin.penalty.dto.PenaltyReq;
import com.green.mmg.admin.penalty.entity.Penalty;
import com.green.mmg.admin.penalty.repository.PenaltyRepository;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;

    // 패널티 부여
    public void givePenalty(PenaltyReq req) {
        Penalty penalty = new Penalty(req);
        penaltyRepository.save(penalty);
    }

    // 패널티 취소
    public void cancelPenalty(Long penaltyId) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ResourceNotFoundException("패널티를 찾을 수 없습니다."));
        penalty.cancel();
        penaltyRepository.save(penalty);
    }
}