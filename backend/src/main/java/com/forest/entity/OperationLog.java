package com.forest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String username;
    private String operation;
    private String method;
    private String url;
    private String ip;
    private String params;
    private String result;
    private String errorMsg;
    private Integer duration;
    private LocalDateTime createdAt;
}
