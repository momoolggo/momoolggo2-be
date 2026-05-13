package com.green.mmg.main.feign.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/** Auth의 InternalUserDetailRes와 동일한 구조임. Feign 응답 역직렬화용으로 Main에 복제 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserDetailRes {
    private Long userNo;
    private String userId;
    private String name;
    private String tel;
    private Integer green;
    private Date createdAt;
}