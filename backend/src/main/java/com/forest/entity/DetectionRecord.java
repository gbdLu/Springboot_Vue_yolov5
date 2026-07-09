package com.forest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("detection_record")
public class DetectionRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer forestAreaId;
    private String imageOriginal;
    private String imageResult;
    private LocalDateTime detectionTime;
    private Integer totalCount;
    private Integer fireCount;
    private Integer humanCount;
    private Integer smokeCount;
    private String resultJson;
    private Integer uploadUserId;
    private Integer status;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String forestAreaName;
    @TableField(exist = false)
    private String uploadUserName;
}