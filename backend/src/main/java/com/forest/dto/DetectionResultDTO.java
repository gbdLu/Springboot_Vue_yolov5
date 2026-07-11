package com.forest.dto;

import lombok.Data;

/**
 * 单个检测目标结果DTO
 */
@Data
public class DetectionResultDTO {
    private Integer classId;
    private String className;
    private Double confidence;
    private double[] bbox;
    private double[] center;
    private Double width;
    private Double height;
}
