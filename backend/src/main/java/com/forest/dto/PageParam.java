package com.forest.dto;

import lombok.Data;

@Data
public class PageParam {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
}
