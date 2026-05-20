package com.green.mmg.admin.policy.service;

import com.green.mmg.admin.policy.dto.PolicyReq;
import com.green.mmg.admin.policy.entity.Policy;
import com.green.mmg.admin.policy.repository.PolicyRepository;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.green.mmg.admin.notification.service.CustomerNotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final CustomerNotificationService customerNotificationService;

    // 정책 목록 조회
    public List<Policy> getPolicyList(String type, Boolean isActive) {
        if (type != null && isActive != null) {
            return policyRepository.findByTypeAndIsActive(type, isActive);
        }
        if (type != null) {
            return policyRepository.findByType(type);
        }
        if (isActive != null) {
            return policyRepository.findByIsActive(isActive);
        }
        return policyRepository.findAll();
    }

    // 정책 등록
    @Transactional
    public void createPolicy(PolicyReq req) {
        Policy policy = policyRepository.save(new Policy(req));

        customerNotificationService.sendPolicyChanged(policy.getTitle());
    }

    // 정책 수정
    @Transactional
    public void updatePolicy(Long policyId, PolicyReq req) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("정책을 찾을 수 없습니다."));
        policy.update(req);

        customerNotificationService.sendPolicyChanged(policy.getTitle());
    }

    // 정책 비활성화
    @Transactional
    public void deactivatePolicy(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("정책을 찾을 수 없습니다."));
        policy.deactivate();

        customerNotificationService.sendPolicyChanged(policy.getTitle());
    }
}