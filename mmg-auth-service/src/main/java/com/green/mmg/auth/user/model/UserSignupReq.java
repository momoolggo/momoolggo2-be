package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignupReq {
    private String name;
    private Long UserNo;
    private String userId;
    private String userPw;
    private Integer gender;
    private String birth;
    private String tel;
    private String role;
    private String address;
    private String addressDetail;
    private Double lat;
    private Double lng;
}
