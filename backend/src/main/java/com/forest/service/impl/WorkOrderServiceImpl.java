package com.forest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forest.entity.WorkOrder;
import com.forest.mapper.WorkOrderMapper;
import com.forest.service.WorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder>
        implements WorkOrderService {
}