package com.green.mmg.main.address;

import com.green.mmg.main.address.model.UserAddressReq;
import com.green.mmg.main.address.model.UserAddressRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAddressMapper {

    // 주소 추가
    int save(UserAddressReq req);

    // 주소 목록 조회
    List<UserAddressRes> findAllByUserNo(long userNo);

    // 기본주소 초기화 (새로 기본주소 설정 전 전부 0으로)
    int resetDefault(long userNo);

    // 주소 수정
    int update(UserAddressReq req);

    // 주소 삭제
    int delete(long addressId);

    //기본배송지로 수정
    void setDefault(@Param("userNo") long userNo, @Param("addressId") long addressId);
}
