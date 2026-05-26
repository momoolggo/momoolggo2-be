package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFindIdReq {
    private String name;
    private String tel;
    private String email;
}
