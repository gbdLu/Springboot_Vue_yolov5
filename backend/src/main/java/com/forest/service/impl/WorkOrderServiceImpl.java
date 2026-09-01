package com.forest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forest.dto.DisposeDTO;
import com.forest.dto.PageParam;
import com.forest.entity.ForestArea;
import com.forest.entity.User;
import com.forest.entity.WorkOrder;
import com.forest.exception.BusinessException;
import com.forest.mapper.WorkOrderMapper;
import com.forest.service.ForestAreaService;
import com.forest.service.NotificationService;
import com.forest.service.UserService;
import com.forest.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.forest.entity.Notification;
import java.time.LocalDateTime;

@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder>
        implements WorkOrderService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ForestAreaService forestAreaService;

    @Override
    public Page<WorkOrder> listWithPermission(PageParam pageParam, Integer orderStatus,
                                               Integer orderType, Integer forestAreaId,
                                               Integer userId, String roleCode) {
        Page<WorkOrder> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();

        // GUARD 只能看自己被指派的工单
        if ("GUARD".equals(roleCode)) {
            wrapper.eq(WorkOrder::getAssignedTo, userId);
        }

        if (pageParam.getKeyword() != null && !pageParam.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(WorkOrder::getOrderNo, pageParam.getKeyword())
                    .or().like(WorkOrder::getHazardType, pageParam.getKeyword()));
        }
        if (orderStatus != null) wrapper.eq(WorkOrder::getOrderStatus, orderStatus);
        if (orderType != null) wrapper.eq(WorkOrder::getOrderType, orderType);
        if (forestAreaId != null) wrapper.eq(WorkOrder::getForestAreaId, forestAreaId);
        wrapper.orderByDesc(WorkOrder::getCreatedAt);

        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public void disposeWithPermission(DisposeDTO dto, Integer userId) {
        WorkOrder order = baseMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException("工单不存在");
        }

        // 只能处置自己名下的工单
        if (!userId.equals(order.getAssignedTo())) {
            throw new BusinessException("只能处置自己名下的工单");
        }
        if (order.getOrderStatus() != 2) {
            throw new BusinessException("当前工单状态不可处置");
        }

        WorkOrder update = new WorkOrder();
        update.setId(dto.getOrderId());
        update.setDisposalDesc(dto.getDisposalDesc());
        update.setDisposalImages(dto.getDisposalImages());
        update.setDisposalAt(LocalDateTime.now());
        update.setOrderStatus(3);  // 已处置，待审核
        update.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(update);

        // 获取巡林员姓名
        User guard = userService.getById(userId);
        String guardName = guard != null ? guard.getRealName() : "巡林员";

        // 获取林区名称
        String areaName = "未知林区";
        if (order.getForestAreaId() != null) {
            ForestArea area = forestAreaService.getById(order.getForestAreaId());
            if (area != null) {
                areaName = area.getAreaName();
            }
        }

        // 通知指派人（林区管理员）进行审核
        if (order.getAssignedBy() != null) {
            Notification noti = new Notification();
            noti.setUserId(order.getAssignedBy());
            noti.setTitle("工单待审核");
            noti.setContent("巡林员" + guardName + "已完成工单" + order.getOrderNo()
                    + "的处置（林区：" + areaName + "），请尽快审核");
            noti.setType("work_order");
            noti.setRelatedId(dto.getOrderId());
            noti.setIsRead(0);
            noti.setCreatedAt(LocalDateTime.now());
            notificationService.save(noti);
        }
    }
}
