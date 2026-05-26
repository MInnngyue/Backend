package com.lostfound.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，唯一
     */
    private String username;

    /**
     * 密码，BCrypt 加密
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 角色：0 普通用户，1 管理员
     */
    private Integer role;

    /**
     * 账号状态：0 正常，1 禁用
     */
    private Integer status;

    /**
     * 信用分，默认 100
     */
    private Integer creditScore;

    /**
     * 成功认领次数
     */
    private Integer successCount;

    private LocalDateTime banUntil;
    private Integer blacklisted;
    private LocalDateTime blacklistUntil;
    private String signature;

    /**
     * 逻辑删除：0 正常，1 删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}