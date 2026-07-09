package com.forest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("work_order")
public class WorkOrder {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderNo;
    private Integer detectionRecordId;
    private Integer forestAreaId;
    private Integer orderType;
    private Integer orderStatus;
    private String hazardType;
    private String hazardDesc;
    private Integer assignedTo;
    private Integer assignedBy;
    private LocalDateTime assignedAt;
    private String disposalDesc;
    private String disposalImages;
    private LocalDateTime disposalAt;
    private Integer reviewResult;
    private String reviewComment;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String forestAreaName;
    @TableField(exist = false)
    private String assignedToName;
    @TableField(exist = false)
    private String assignedByName;
}