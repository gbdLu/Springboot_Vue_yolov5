package com.forest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forest.dto.PageParam;
import com.forest.dto.Result;
import com.forest.entity.ForestArea;
import com.forest.entity.MonitoringPoint;
import com.forest.entity.User;
import com.forest.service.ForestAreaService;
import com.forest.service.MonitoringPointService;
import com.forest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/forest")
public class ForestAreaController {

    @Autowired
    private ForestAreaService forestAreaService;

    @Autowired
    private MonitoringPointService monitoringPointService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(PageParam pageParam,
                                             @RequestParam(required = false) Integer fireRiskLevel,
                                             @RequestParam(required = false) Integer status) {
        Page<ForestArea> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        LambdaQueryWrapper<ForestArea> wrapper = new LambdaQueryWrapper<>();
        if (pageParam.getKeyword() != null && !pageParam.getKeyword().isEmpty()) {
            wrapper.like(ForestArea::getAreaName, pageParam.getKeyword())
                   .or().like(ForestArea::getLocation, pageParam.getKeyword());
        }
        if (fireRiskLevel != null) {
            wrapper.eq(ForestArea::getFireRiskLevel, fireRiskLevel);
        }
        if (status != null) {
            wrapper.eq(ForestArea::getStatus, status);
        }
        wrapper.orderByDesc(ForestArea::getCreatedAt);
        Page<ForestArea> result = forestAreaService.page(page, wrapper);

        // 填充管理员姓名
        result.getRecords().forEach(area -> {
            if (area.getManagerId() != null) {
                User manager = userService.getById(area.getManagerId());
                if (manager != null) {
                    area.setManagerName(manager.getRealName());
                }
            }
        });

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody ForestArea area) {
        area.setCreatedAt(LocalDateTime.now());
        area.setUpdatedAt(LocalDateTime.now());
        if (area.getStatus() == null) area.setStatus(1);
        forestAreaService.save(area);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody ForestArea area) {
        area.setUpdatedAt(LocalDateTime.now());
        forestAreaService.updateById(area);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        forestAreaService.removeById(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/export")
    public Result<List<ForestArea>> export() {
        List<ForestArea> list = forestAreaService.list();
        list.forEach(area -> {
            if (area.getManagerId() != null) {
                User manager = userService.getById(area.getManagerId());
                if (manager != null) {
                    area.setManagerName(manager.getRealName());
                }
            }
        });
        return Result.success(list);
    }

    // ============ 监控点管理 ============

    @GetMapping("/{areaId}/points")
    public Result<List<MonitoringPoint>> listPoints(@PathVariable Integer areaId) {
        LambdaQueryWrapper<MonitoringPoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonitoringPoint::getForestAreaId, areaId);
        wrapper.orderByDesc(MonitoringPoint::getCreatedAt);
        List<MonitoringPoint> points = monitoringPointService.list(wrapper);
        return Result.success(points);
    }

    @PostMapping("/point/add")
    public Result<Void> addPoint(@RequestBody MonitoringPoint point) {
        point.setCreatedAt(LocalDateTime.now());
        monitoringPointService.save(point);
        return Result.success("监控点添加成功", null);
    }

    @PutMapping("/point/update")
    public Result<Void> updatePoint(@RequestBody MonitoringPoint point) {
        monitoringPointService.updateById(point);
        return Result.success("监控点更新成功", null);
    }

    @DeleteMapping("/point/delete/{id}")
    public Result<Void> deletePoint(@PathVariable Integer id) {
        monitoringPointService.removeById(id);
        return Result.success("监控点删除成功", null);
    }

    @GetMapping("/point/detail/{id}")
    public Result<MonitoringPoint> getPointDetail(@PathVariable Integer id) {
        MonitoringPoint point = monitoringPointService.getById(id);
        if (point == null) {
            return Result.error("监控点不存在");
        }
        return Result.success(point);
    }
}
