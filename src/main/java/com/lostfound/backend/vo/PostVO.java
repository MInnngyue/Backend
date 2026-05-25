package com.lostfound.backend.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostVO {

    private Long id;
    private Integer type;
    private String itemCategory;
    private String color;
    private String locationCampus;
    private String locationArea;
    private String locationDetail;
    private LocalDate lostTime;
    private String title;
    private String description;
    private Integer status;
    private Integer viewCount;
    private Integer reviewStatus;
    private String coverImage;
    private List<String> images;

    private Long userId;
    private String nickname;
    private String avatar;
    private Integer creditScore;

    private LocalDateTime createTime;
}
