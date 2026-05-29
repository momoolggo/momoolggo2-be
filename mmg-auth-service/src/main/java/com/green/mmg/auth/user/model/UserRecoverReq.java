package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRecoverReq {
    private String userId;
    private String userPw;
}
