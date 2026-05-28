package com.lostfound.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    /* 0=user 1=admin */
    private Integer role;

    /* 0=ok 1=banned */
    private Integer status;

    private Integer creditScore;

    private Integer successCount;

    private LocalDateTime banUntil;
    private Integer blacklisted;
    private LocalDateTime blacklistUntil;
    private String signature;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}