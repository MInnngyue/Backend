package com.lostfound.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer type;

    private String itemCategory;

    private String color;

    private String locationCampus;

    private String locationArea;

    private String locationDetail;

    private LocalDate lostTime;

    private String title;

    private String description;

    /* 0=进行中 1=匹配 2=认领中 3=完结 4=过期 5=下架 */
    private Integer status;

    private Integer viewCount;

    /* 0=待审 1=通过 2=拒绝 */
    private Integer reviewStatus;

    private String reviewRemark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
