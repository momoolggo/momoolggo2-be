package com.green.mmg.main.ownerprofile;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.ownerprofile.dto.OwnerProfileCreateReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerProfileService {

    private final OwnerProfileRepository ownerProfileRepository;

    @Transactional
    public void create(OwnerProfileCreateReq req) {
        validate(req);

        if (ownerProfileRepository.existsByUserNo(req.getUserNo())) {
            throw new BusinessException("이미 등록된 사장 프로필입니다.", HttpStatus.CONFLICT);
        }

        OwnerProfile profile = new OwnerProfile(
                req.getUserNo(),
                req.getStoreAddress(),
                req.getBusinessNumber(),
                req.getBusinessLicenseUrl(),
                req.getMailOrderLicenseUrl(),
                req.getBankName(),
                req.getAccountNumber(),
                req.getAccountHolder()
        );

        ownerProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public OwnerProfile getByUserNo(Long userNo) {
        return ownerProfileRepository.findByUserNo(userNo)
                .orElseThrow(() -> new BusinessException("사장 프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private void validate(OwnerProfileCreateReq req) {
        requireNonBlank(req.getStoreAddress(), "주소는 필수입니다.");
        requireNonBlank(req.getBusinessNumber(), "사업자 등록 번호는 필수입니다.");
        requireNonBlank(req.getBusinessLicenseUrl(), "영업 신고증은 필수입니다.");
        requireNonBlank(req.getMailOrderLicenseUrl(), "통신판매업 신고증은 필수입니다.");
        requireNonBlank(req.getBankName(), "정산 은행은 필수입니다.");
        requireNonBlank(req.getAccountNumber(), "정산 계좌번호는 필수입니다.");
        requireNonBlank(req.getAccountHolder(), "예금주는 필수입니다.");
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }
    }
}