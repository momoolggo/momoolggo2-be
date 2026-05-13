package com.green.mmg.auth.internal.dto;

import com.green.mmg.auth.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@AllArgsConstructor

public class InternalAdminUserRes {
    private Long userNo;
    private String userId;
    private String name;
    private String tel;
    private String role;
    private String status;
    private Date createdAt;
    private Integer green;

    public static InternalAdminUserRes from(User user) {
        return new InternalAdminUserRes(
                user.getUserNo(),
                user.getUserId(),
                user.getName(),
                user.getTel(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getGreen()
        );
    }
}

