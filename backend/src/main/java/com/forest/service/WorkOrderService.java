package com.forest.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.forest.dto.DisposeDTO;
import com.forest.dto.PageParam;
import com.forest.entity.WorkOrder;

public interface WorkOrderService extends IService<WorkOrder> {

    /**
     * 工单列表（带权限过滤）
     * GUARD 只看自己被指派的工单；MANAGER/ADMIN 看所有
     *
     * @param pageParam    分页参数
     * @param orderStatus  工单状态筛选
     * @param orderType    工单类型筛选
     * @param forestAreaId 林区筛选
     * @param userId       当前用户ID
     * @param roleCode     当前用户角色
     * @return 分页结果
     */
    Page<WorkOrder> listWithPermission(PageParam pageParam, Integer orderStatus,
                                        Integer orderType, Integer forestAreaId,
                                        Integer userId, String roleCode);

    /**
     * 处置工单（带权限校验）
     * 仅 GUARD 可操作，且只能处置自己名下的工单
     *
     * @param dto    处置信息
     * @param userId 当前用户ID
     */
    void disposeWithPermission(DisposeDTO dto, Integer userId);
}
