package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateReq {
    private long   userNo;  // JWT에서 꺼내서 서비스에서 넣어줄 거예요
    private String name;
    private String userPw;
    private String tel;
    private int    gender;
    private String birth;
}
