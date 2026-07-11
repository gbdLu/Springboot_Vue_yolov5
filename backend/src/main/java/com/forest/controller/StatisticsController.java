package com.forest.controller;

import com.forest.dto.Result;
import com.forest.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(statisticsService.getOverview());
    }

    @GetMapping("/pie")
    public Result<List<Map<String, Object>>> getPieChart() {
        return Result.success(statisticsService.getPieChart());
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend() {
        return Result.success(statisticsService.getTrend());
    }

    @GetMapping("/bar")
    public Result<List<Map<String, Object>>> getBarChart() {
        return Result.success(statisticsService.getBarChart());
    }

    @GetMapping("/efficiency")
    public Result<Map<String, Object>> getEfficiency() {
        return Result.success(statisticsService.getEfficiency());
    }

    @GetMapping("/high-risk")
    public Result<List<Map<String, Object>>> getHighRisk() {
        return Result.success(statisticsService.getHighRisk());
    }

    @GetMapping("/heatmap")
    public Result<List<Map<String, Object>>> getHeatmap() {
        return Result.success(statisticsService.getHeatmap());
    }
}
