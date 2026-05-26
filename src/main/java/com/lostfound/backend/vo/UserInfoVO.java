package com.lostfound.backend.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private Integer role;

    private Integer status;

    private Integer creditScore;

    private Integer successCount;

    private String signature;
}