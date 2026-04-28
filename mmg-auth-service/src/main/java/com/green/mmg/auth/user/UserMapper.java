package com.green.mmg.auth.user;

import com.green.mmg.auth.user.model.*;
import com.green.mmg.common.dto.feign.UserBriefDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    // ===== Internal API용 (Phase 4-A) =====
    // /internal/auth/user/{userNo}
    UserBriefDto findBriefByUserNo(long userNo);

    // /internal/auth/users?ids=...
    List<UserBriefDto> findBriefsByUserNos(@Param("ids") List<Long> userNos);

    // 리뷰 메서드는 Phase 2에서 main-service에 작성
}
