package com.lostfound.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("claim")
public class Claim {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long claimUserId;
    private Long postOwnerId;
    private Long matchId;

    /* 0=待确认 1=一方确认 2=完结 3=取消 */
    private Integer status;

    private Integer ownerConfirmed;
    private Integer claimerConfirmed;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
