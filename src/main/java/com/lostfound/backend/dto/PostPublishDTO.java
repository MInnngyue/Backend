package com.lostfound.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PostPublishDTO {

    @NotNull(message = "帖子类型不能为空")
    private Integer type;

    @NotBlank(message = "物品大类不能为空")
    private String itemCategory;

    @NotBlank(message = "颜色不能为空")
    private String color;

    @NotBlank(message = "校区不能为空")
    private String locationCampus;

    private String locationArea;
    private String locationDetail;

    @NotNull(message = "时间不能为空")
    private LocalDate lostTime;

    private String title;
    private String description;
    private String[] images;
}
