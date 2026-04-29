package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateReq {
    private long    userNo;  // JWT에서 꺼내서 서비스에서 넣어줄 거예요
    private String  name;
    private String  userPw;
    private String  tel;
    /** 미전송(null)과 0("선택 안 함"으로 변경)을 구분하기 위해 Integer */
    private Integer gender;
    private String  birth;
}
