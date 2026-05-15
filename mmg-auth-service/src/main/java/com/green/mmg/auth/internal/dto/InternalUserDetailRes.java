package com.green.mmg.auth.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
public class InternalUserDetailRes {
    private Long userNo;
    private String userId;
    private String name;
    private String tel;
    private Integer green;
    private Date createdAt;
    private String status;


}
