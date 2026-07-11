package com.forest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("monitoring_point")
public class MonitoringPoint {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer forestAreaId;
    private String pointName;
    private String pointType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String description;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String forestAreaName;
}
