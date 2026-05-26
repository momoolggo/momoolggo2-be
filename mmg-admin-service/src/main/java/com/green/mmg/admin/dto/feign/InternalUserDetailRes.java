package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Date;

@Getter
@NoArgsConstructor
public class InternalUserDetailRes {
    private Long userNo;
    private String userId;
    private String name;
    private String tel;
    private Integer green;
    private Date createdAt;
    private String status;
    private String role;
}