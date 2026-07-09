package com.forest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String title;
    private String content;
    private String type;
    private Integer relatedId;
    private Integer isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}