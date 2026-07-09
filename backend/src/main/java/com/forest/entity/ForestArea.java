package com.forest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@TableName("forest_area")
public class ForestArea {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String areaName;
    private String location;
    private Integer fireRiskLevel;
    private Integer managerId;
    private BigDecimal totalArea;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String managerName;
}