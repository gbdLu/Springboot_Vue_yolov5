package com.forest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forest.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
}