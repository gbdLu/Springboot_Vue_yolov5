package com.forest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forest.entity.MonitoringPoint;
import com.forest.mapper.MonitoringPointMapper;
import com.forest.service.MonitoringPointService;
import org.springframework.stereotype.Service;

@Service
public class MonitoringPointServiceImpl extends ServiceImpl<MonitoringPointMapper, MonitoringPoint>
        implements MonitoringPointService {
}
