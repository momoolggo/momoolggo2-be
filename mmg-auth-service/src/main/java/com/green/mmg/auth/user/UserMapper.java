package com.green.mmg.auth.user;

import com.green.mmg.auth.user.model.*;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    // 회원가입
    int signup(UserSignupReq req);

    // 로그인
    User findByUserId(String userId);

    // 아이디 중복확인
    int countByUserId(String userId);

    User findByUserNo(Long userNo);

    int update(UserUpdateReq req);

    // 리뷰 메서드는 Phase 2에서 main-service에 작성
}
