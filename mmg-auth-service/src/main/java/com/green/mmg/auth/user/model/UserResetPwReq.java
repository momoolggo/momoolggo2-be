package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResetPwReq {
    private String userId;
    private String name;
    private String tel;
    private String email;
    private String newPassword;
    private String verificationCode;
}
