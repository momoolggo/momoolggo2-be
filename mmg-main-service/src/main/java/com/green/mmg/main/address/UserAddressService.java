package com.green.mmg.main.address;

import com.green.mmg.main.address.model.UserAddressReq;
import com.green.mmg.main.address.model.UserAddressRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressMapper userAddressMapper;

    // ── 주소 추가
    @Transactional
    public void save(long userNo, UserAddressReq req) {
        req.setUserNo(userNo);

        if (req.getDefaultAd() != null && req.getDefaultAd() == 1) {
            userAddressMapper.resetDefault(userNo);
        }
        userAddressMapper.save(req);
    }

    // ── 주소 목록 조회
    public List<UserAddressRes> findAll(long userNo) {
        return userAddressMapper.findAllByUserNo(userNo);
    }

    // ── 주소 수정
    @Transactional
    public void update(long userNo, UserAddressReq req) {
        if (req.getDefaultAd() != null && req.getDefaultAd() == 1) {
            userAddressMapper.resetDefault(userNo);
        }
        userAddressMapper.update(req);
    }

    // ── 주소 삭제
    public void delete(long addressId) {
        userAddressMapper.delete(addressId);
    }

    // 기본 배송지로 수정
    public void setDefault(long userNo, long addressId) {
        userAddressMapper.setDefault(userNo, addressId);
    }
}
