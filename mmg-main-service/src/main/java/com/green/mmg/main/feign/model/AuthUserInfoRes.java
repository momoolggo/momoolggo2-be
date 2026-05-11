package com.green.mmg.main.feign.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthUserInfoRes {
    private Long userNo;
    private String name;
    private String tel;
    private String address;
}