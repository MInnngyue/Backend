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

    /** 0=寻物启事, 1=失物招领 */
    private Integer type;

    private String itemCategory;

    private String color;

    private String locationCampus;

    private String locationArea;

    private String locationDetail;

    private LocalDate lostTime;

    private String title;

    private String description;

    /** 0=进行中, 1=已匹配, 2=认领中, 3=已完结, 4=已过期, 5=已下架 */
    private Integer status;

    private Integer viewCount;

    /** 0=待审核, 1=已通过, 2=已拒绝 */
    private Integer reviewStatus;

    private String reviewRemark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
