package com.forest.dto;

import lombok.Data;

@Data
public class ReviewDTO {
    private Integer orderId;
    private Integer reviewResult;  // 1=通过, 2=退回
    private String reviewComment;
}
