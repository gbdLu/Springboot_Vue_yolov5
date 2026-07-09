package com.forest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Integer roleId;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String roleName;
    @TableField(exist = false)
    private String roleCode;
}