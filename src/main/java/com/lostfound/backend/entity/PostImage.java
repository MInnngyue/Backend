package com.lostfound.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_image")
public class PostImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private String imageUrl;

    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
