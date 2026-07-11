package com.forest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forest.entity.*;
import com.forest.mapper.*;
import com.forest.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private DetectionRecordMapper detectionRecordMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private ForestAreaMapper forestAreaMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        // 今日识别数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<DetectionRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(DetectionRecord::getCreatedAt, todayStart);
        Long todayDetections = detectionRecordMapper.selectCount(todayWrapper);
        result.put("todayDetections", todayDetections);

        // 待处理工单数
        LambdaQueryWrapper<WorkOrder> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(WorkOrder::getOrderStatus, 1);
        Long pendingOrders = workOrderMapper.selectCount(pendingWrapper);
        result.put("pendingOrders", pendingOrders);

        // 总林区数
        Long totalForests = forestAreaMapper.selectCount(null);
        result.put("totalForests", totalForests);

        // 本月火情数
        LocalDateTime monthStart = LocalDateTime.of(YearMonth.now().atDay(1), LocalTime.MIN);
        LambdaQueryWrapper<WorkOrder> fireWrapper = new LambdaQueryWrapper<>();
        fireWrapper.ge(WorkOrder::getCreatedAt, monthStart)
                   .like(WorkOrder::getHazardType, "明火");
        Long monthFire = workOrderMapper.selectCount(fireWrapper);
        result.put("monthFire", monthFire);

        // 总识别数
        Long totalDetections = detectionRecordMapper.selectCount(null);
        result.put("totalDetections", totalDetections);

        // 已处理工单数
        LambdaQueryWrapper<WorkOrder> doneWrapper = new LambdaQueryWrapper<>();
        doneWrapper.ge(WorkOrder::getOrderStatus, 4);
        Long doneOrders = workOrderMapper.selectCount(doneWrapper);
        result.put("doneOrders", doneOrders);

        return result;
    }

    @Override
    public List<Map<String, Object>> getPieChart() {
        List<Map<String, Object>> result = new ArrayList<>();

        LambdaQueryWrapper<WorkOrder> fireWrapper = new LambdaQueryWrapper<>();
        fireWrapper.like(WorkOrder::getHazardType, "明火");
        Long fireCount = workOrderMapper.selectCount(fireWrapper);

        LambdaQueryWrapper<WorkOrder> smokeWrapper = new LambdaQueryWrapper<>();
        smokeWrapper.like(WorkOrder::getHazardType, "烟雾");
        Long smokeCount = workOrderMapper.selectCount(smokeWrapper);

        LambdaQueryWrapper<WorkOrder> humanWrapper = new LambdaQueryWrapper<>();
        humanWrapper.like(WorkOrder::getHazardType, "违规");
        Long humanCount = workOrderMapper.selectCount(humanWrapper);

        Map<String, Object> fire = new HashMap<>();
        fire.put("name", "明火");
        fire.put("value", fireCount);
        result.add(fire);

        Map<String, Object> smoke = new HashMap<>();
        smoke.put("name", "烟雾");
        smoke.put("value", smokeCount);
        result.add(smoke);

        Map<String, Object> human = new HashMap<>();
        human.put("name", "违规用火");
        human.put("value", humanCount);
        result.add(human);

        return result;
    }

    @Override
    public List<Map<String, Object>> getTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<DetectionRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(DetectionRecord::getCreatedAt, dayStart)
                   .le(DetectionRecord::getCreatedAt, dayEnd);
            Long count = detectionRecordMapper.selectCount(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(formatter));
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getBarChart() {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM月");

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime monthStart = LocalDateTime.of(ym.atDay(1), LocalTime.MIN);
            LocalDateTime monthEnd = LocalDateTime.of(ym.atEndOfMonth(), LocalTime.MAX);

            LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(WorkOrder::getCreatedAt, monthStart)
                   .le(WorkOrder::getCreatedAt, monthEnd);
            Long count = workOrderMapper.selectCount(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("month", ym.format(formatter));
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getEfficiency() {
        Map<String, Object> result = new HashMap<>();

        // 已归档工单的平均处置时长（小时）
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(WorkOrder::getClosedAt)
               .isNotNull(WorkOrder::getCreatedAt);
        List<WorkOrder> closedOrders = workOrderMapper.selectList(wrapper);

        double avgHours = 0;
        if (!closedOrders.isEmpty()) {
            long totalMinutes = 0;
            int count = 0;
            for (WorkOrder order : closedOrders) {
                if (order.getClosedAt() != null && order.getCreatedAt() != null) {
                    totalMinutes += java.time.Duration.between(order.getCreatedAt(), order.getClosedAt()).toMinutes();
                    count++;
                }
            }
            if (count > 0) {
                avgHours = Math.round(totalMinutes / count / 60.0 * 10) / 10.0;
            }
        }
        result.put("avgHandleHours", avgHours);

        // 处置率
        Long totalOrders = workOrderMapper.selectCount(null);
        Long closedOrders2 = workOrderMapper.selectCount(
            new LambdaQueryWrapper<WorkOrder>().isNotNull(WorkOrder::getClosedAt));
        double handleRate = totalOrders > 0 ? Math.round(closedOrders2 * 1000.0 / totalOrders) / 10.0 : 0;
        result.put("handleRate", handleRate);

        // 审核通过率
        LambdaQueryWrapper<WorkOrder> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.isNotNull(WorkOrder::getReviewResult);
        Long reviewedCount = workOrderMapper.selectCount(reviewWrapper);
        LambdaQueryWrapper<WorkOrder> passWrapper = new LambdaQueryWrapper<>();
        passWrapper.eq(WorkOrder::getReviewResult, 1);
        Long passCount = workOrderMapper.selectCount(passWrapper);
        double passRate = reviewedCount > 0 ? Math.round(passCount * 1000.0 / reviewedCount) / 10.0 : 0;
        result.put("passRate", passRate);

        return result;
    }

    @Override
    public List<Map<String, Object>> getHighRisk() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 按林区统计工单数量，取前5
        List<ForestArea> areas = forestAreaMapper.selectList(null);
        for (ForestArea area : areas) {
            LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WorkOrder::getForestAreaId, area.getId());
            Long count = workOrderMapper.selectCount(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("areaName", area.getAreaName());
            item.put("orderCount", count);
            item.put("fireRiskLevel", area.getFireRiskLevel());
            result.add(item);
        }

        // 按工单数排序
        result.sort((a, b) -> Long.compare((Long) b.get("orderCount"), (Long) a.get("orderCount")));
        return result.size() > 5 ? result.subList(0, 5) : result;
    }

    @Override
    public List<Map<String, Object>> getHeatmap() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 按林区统计工单热度数据
        List<ForestArea> areas = forestAreaMapper.selectList(null);
        for (ForestArea area : areas) {
            LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WorkOrder::getForestAreaId, area.getId());
            Long count = workOrderMapper.selectCount(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("areaId", area.getId());
            item.put("areaName", area.getAreaName());
            item.put("location", area.getLocation());
            item.put("orderCount", count);
            item.put("fireRiskLevel", area.getFireRiskLevel());
            result.add(item);
        }

        return result;
    }
}
