package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class UserGetRes {
    private String userId;
    private String name;
    private String tel;
    private String email;
    private int    gender;
    private String birth;
    private Integer green;

    public UserGetRes(String userId, String name, String tel, int gender, String birth) {
        this(userId, name, tel, null, gender, birth, 0);
    }
}
