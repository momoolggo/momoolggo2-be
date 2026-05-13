package com.green.mmg.main.address;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.address.model.UserAddress;
import com.green.mmg.main.address.model.UserAddressReq;
import com.green.mmg.main.address.model.UserAddressRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Phase 3-D-B: 전 6 SQL JPA 전환 + UserAddressMapper / Address.xml 제거.
 * dirty checking으로 update/setDefault 단순화.
 */
@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;

    @Transactional
    public void save(long userNo, UserAddressReq req) {
        if (req.getDefaultAd() != null && req.getDefaultAd() == 1) {
            userAddressRepository.resetDefault(userNo);
            userAddressRepository.flush();
        }
        UserAddress entity = new UserAddress();
        entity.setUserNo(userNo);
        entity.setAddress(req.getAddress());
        entity.setAddressDetail(req.getAddressDetail());
        entity.setLatitude(req.getLatitude());
        entity.setLongitude(req.getLongitude());
        entity.setDefaultAd(req.getDefaultAd());
        userAddressRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<UserAddressRes> findAll(long userNo) {
        return userAddressRepository.findAllByUserNo(userNo);
    }

    @Transactional
    public void update(long callerUserNo, UserAddressReq req) {
        UserAddress entity = userAddressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new BusinessException("주소를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        // Phase 3-Backfill-A-5: 소유자 검증 (delete 패턴 일관 적용)
        if (!Objects.equals(entity.getUserNo(), callerUserNo)) {
            throw new BusinessException("본인 주소만 수정 가능합니다.", HttpStatus.FORBIDDEN);
        }
        if (req.getDefaultAd() != null && req.getDefaultAd() == 1) {
            userAddressRepository.resetDefault(callerUserNo);
            userAddressRepository.flush();
        }
        entity.setAddress(req.getAddress());
        entity.setAddressDetail(req.getAddressDetail());
        entity.setLatitude(req.getLatitude());
        entity.setLongitude(req.getLongitude());
        entity.setDefaultAd(req.getDefaultAd());
        // dirty checking으로 자동 UPDATE
    }

    @Transactional
    public void delete(long callerUserNo, long addressId) {
        UserAddress entity = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("주소를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(entity.getUserNo(), callerUserNo)) {
            throw new BusinessException("본인 주소만 삭제 가능합니다.", HttpStatus.FORBIDDEN);
        }
        userAddressRepository.delete(entity);
    }

    @Transactional
    public void setDefault(long userNo, long addressId) {
        userAddressRepository.resetDefault(userNo);
        userAddressRepository.flush();
        UserAddress entity = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("주소를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        entity.setDefaultAd(1);  // dirty checking
    }
}
