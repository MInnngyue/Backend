package com.lostfound.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("match_record")
public class MatchRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lostPostId;

    private Long foundPostId;

    private Integer score;

    private Integer itemScore;
    private Integer colorScore;
    private Integer locationScore;
    private Integer timeScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
