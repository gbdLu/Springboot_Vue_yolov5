package com.forest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forest.dto.PageParam;
import com.forest.dto.Result;
import com.forest.entity.Notification;
import com.forest.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(PageParam pageParam,
                                             @RequestAttribute("userId") Integer userId) {
        Page<Notification> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.orderByDesc(Notification::getCreatedAt);
        Page<Notification> result = notificationService.page(page, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestAttribute("userId") Integer userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0);
        Long count = notificationService.count(wrapper);
        return Result.success(count);
    }

    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Integer id) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, id)
               .set(Notification::getIsRead, 1)
               .set(Notification::getReadAt, LocalDateTime.now());
        notificationService.update(wrapper);
        return Result.success("已读", null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestAttribute("userId") Integer userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0)
               .set(Notification::getIsRead, 1)
               .set(Notification::getReadAt, LocalDateTime.now());
        notificationService.update(wrapper);
        return Result.success("全部已读", null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        notificationService.removeById(id);
        return Result.success("删除成功", null);
    }
}
