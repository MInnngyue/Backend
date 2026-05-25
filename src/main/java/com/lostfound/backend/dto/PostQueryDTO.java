package com.lostfound.backend.dto;

import lombok.Data;

@Data
public class PostQueryDTO {

    private Integer type;
    private String itemCategory;
    private Integer status;
    private String keyword;
    private Integer page = 1;
    private Integer pageSize = 10;
}
