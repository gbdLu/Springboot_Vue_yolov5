package com.forest.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {
    Map<String, Object> getOverview();
    List<Map<String, Object>> getPieChart();
    List<Map<String, Object>> getTrend();
    List<Map<String, Object>> getBarChart();
    Map<String, Object> getEfficiency();
    List<Map<String, Object>> getHighRisk();
    List<Map<String, Object>> getHeatmap();
}
