package com.forest.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forest.dto.*;
import com.forest.entity.*;
import com.forest.mapper.RoleMapper;
import com.forest.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/workorder")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private ForestAreaService forestAreaService;

    @Autowired
    private UserService userService;

    @Autowired
    private DetectionService detectionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RoleMapper roleMapper;

    /**
     * 工单列表（GUARD 只看自己被指派的工单，Service层过滤）
     * GUARD 只能看到 assigned_to = 自己的工单；MANAGER/ADMIN 看所有
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(PageParam pageParam,
                                             @RequestParam(required = false) Integer orderStatus,
                                             @RequestParam(required = false) Integer orderType,
                                             @RequestParam(required = false) Integer forestAreaId,
                                             @RequestAttribute("userId") Integer userId,
                                             @RequestAttribute("roleCode") String roleCode) {
        Page<WorkOrder> result = workOrderService.listWithPermission(
                pageParam, orderStatus, orderType, forestAreaId, userId, roleCode);

        fillWorkOrderInfo(result.getRecords());

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    /**
     * 工单详情
     * GUARD 只能查看自己的工单
     */
    @GetMapping("/detail/{id}")
    public Result<WorkOrder> getDetail(@PathVariable Integer id,
                                       @RequestAttribute("userId") Integer userId,
                                       @RequestAttribute("roleCode") String roleCode) {
        WorkOrder order = workOrderService.getById(id);
        if (order == null) return Result.error("工单不存在");

        // GUARD 只能看自己的工单
        if ("GUARD".equals(roleCode) && !userId.equals(order.getAssignedTo())) {
            return Result.error("无权查看此工单");
        }

        fillWorkOrderInfo(Collections.singletonList(order));
        return Result.success(order);
    }

    /**
     * 指派工单 — 仅 MANAGER
     */
    @PostMapping("/assign")
    public Result<Void> assign(@RequestBody AssignDTO dto,
                                @RequestAttribute("userId") Integer userId,
                                @RequestAttribute("roleCode") String roleCode) {
        if (!"MANAGER".equals(roleCode)) {
            return Result.error("仅林区管理员可指派工单");
        }

        WorkOrder order = workOrderService.getById(dto.getOrderId());
        if (order == null) return Result.error("工单不存在");
        if (order.getOrderStatus() != 1) {
            return Result.error("当前工单状态不可指派");
        }

        // 校验被指派人必须是 GUARD
        User targetUser = userService.getById(dto.getAssignedTo());
        if (targetUser == null) {
            return Result.error("被指派用户不存在");
        }
        if (targetUser.getRoleId() != null) {
            Role targetRole = roleMapper.selectById(targetUser.getRoleId());
            if (targetRole == null || !"GUARD".equals(targetRole.getRoleCode())) {
                return Result.error("只能指派给护林员（GUARD）");
            }
        } else {
            return Result.error("该用户未分配角色，无法指派");
        }

        WorkOrder update = new WorkOrder();
        update.setId(dto.getOrderId());
        update.setAssignedTo(dto.getAssignedTo());
        update.setAssignedBy(userId);
        update.setAssignedAt(LocalDateTime.now());
        update.setOrderStatus(2);  // 已指派
        update.setUpdatedAt(LocalDateTime.now());
        workOrderService.updateById(update);

        // 发送消息通知被指派人
        Notification noti = new Notification();
        noti.setUserId(dto.getAssignedTo());
        noti.setTitle("新工单指派");
        noti.setContent("您有新的工单待处理，工单号：" + order.getOrderNo());
        noti.setType("work_order");
        noti.setRelatedId(dto.getOrderId());
        noti.setIsRead(0);
        noti.setCreatedAt(LocalDateTime.now());
        notificationService.save(noti);

        return Result.success("指派成功", null);
    }

    /**
     * 处置工单 — 仅 GUARD，且只能处置自己名下的工单（Service层校验）
     */
    @PostMapping("/dispose")
    public Result<Void> dispose(@RequestBody DisposeDTO dto,
                                 @RequestAttribute("userId") Integer userId,
                                 @RequestAttribute("roleCode") String roleCode) {
        if (!"GUARD".equals(roleCode)) {
            return Result.error("仅巡护员可处置工单");
        }
        try {
            workOrderService.disposeWithPermission(dto, userId);
            return Result.success("处置提交成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核工单 — 仅 MANAGER
     */
    @PostMapping("/review")
    public Result<Void> review(@RequestBody ReviewDTO dto,
                                @RequestAttribute("userId") Integer userId,
                                @RequestAttribute("roleCode") String roleCode) {
        if (!"MANAGER".equals(roleCode)) {
            return Result.error("仅林区管理员可审核工单");
        }

        WorkOrder order = workOrderService.getById(dto.getOrderId());
        if (order == null) return Result.error("工单不存在");
        if (order.getOrderStatus() != 3) {
            return Result.error("当前工单状态不可审核");
        }

        WorkOrder update = new WorkOrder();
        update.setId(dto.getOrderId());
        update.setReviewResult(dto.getReviewResult());
        update.setReviewComment(dto.getReviewComment());
        update.setReviewedBy(userId);
        update.setReviewedAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());

        if (dto.getReviewResult() == 1) {
            // 审核通过 → 归档
            update.setOrderStatus(4);
            update.setClosedAt(LocalDateTime.now());
        } else {
            // 审核退回 → 重新指派（已指派状态）
            update.setOrderStatus(2);
        }
        workOrderService.updateById(update);

        // 通知处置人
        if (order.getAssignedTo() != null) {
            Notification noti = new Notification();
            noti.setUserId(order.getAssignedTo());
            noti.setTitle(dto.getReviewResult() == 1 ? "工单已归档" : "工单已退回");
            noti.setContent("工单 " + order.getOrderNo() + (dto.getReviewResult() == 1 ? " 审核通过已归档" : " 审核退回，请重新处置"));
            noti.setType("work_order");
            noti.setRelatedId(dto.getOrderId());
            noti.setIsRead(0);
            noti.setCreatedAt(LocalDateTime.now());
            notificationService.save(noti);
        }

        return Result.success("审核完成", null);
    }

    /**
     * 按状态筛选
     * GUARD 只能看自己的工单
     */
    @GetMapping("/filter")
    public Result<Map<String, Object>> filterByStatus(PageParam pageParam,
                                                       @RequestParam Integer status,
                                                       @RequestAttribute("userId") Integer userId,
                                                       @RequestAttribute("roleCode") String roleCode) {
        Page<WorkOrder> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();

        // GUARD 只能看自己的工单
        if ("GUARD".equals(roleCode)) {
            wrapper.eq(WorkOrder::getAssignedTo, userId);
        }

        wrapper.eq(WorkOrder::getOrderStatus, status);
        wrapper.orderByDesc(WorkOrder::getCreatedAt);
        Page<WorkOrder> result = workOrderService.page(page, wrapper);
        fillWorkOrderInfo(result.getRecords());

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    /**
     * 导出工单 — 仅 MANAGER/ADMIN
     */
    @GetMapping("/export")
    public Result<?> export(@RequestAttribute("roleCode") String roleCode) {
        if ("GUARD".equals(roleCode)) {
            return Result.error("巡护员无权导出工单");
        }
        List<WorkOrder> list = workOrderService.list();
        fillWorkOrderInfo(list);
        return Result.success(list);
    }

    /**
     * 我的工单 — 所有角色可用，返回 assigned_to = 当前用户的工单
     */
    @GetMapping("/my-orders")
    public Result<Map<String, Object>> getMyOrders(PageParam pageParam,
                                                    @RequestAttribute("userId") Integer userId) {
        Page<WorkOrder> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getAssignedTo, userId);
        wrapper.orderByDesc(WorkOrder::getCreatedAt);
        Page<WorkOrder> result = workOrderService.page(page, wrapper);
        fillWorkOrderInfo(result.getRecords());

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    /**
     * 工单统计 — 仅 MANAGER/ADMIN
     */
    @GetMapping("/statistics")
    public Result<?> getStatistics(@RequestAttribute("roleCode") String roleCode) {
        if ("GUARD".equals(roleCode)) {
            return Result.error("巡护员无权查看工单统计");
        }

        Map<String, Object> data = new HashMap<>();

        LambdaQueryWrapper<WorkOrder> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(WorkOrder::getOrderStatus, 1);
        data.put("pendingCount", workOrderService.count(pendingWrapper));

        LambdaQueryWrapper<WorkOrder> assignedWrapper = new LambdaQueryWrapper<>();
        assignedWrapper.eq(WorkOrder::getOrderStatus, 2);
        data.put("assignedCount", workOrderService.count(assignedWrapper));

        LambdaQueryWrapper<WorkOrder> disposedWrapper = new LambdaQueryWrapper<>();
        disposedWrapper.eq(WorkOrder::getOrderStatus, 3);
        data.put("disposedCount", workOrderService.count(disposedWrapper));

        LambdaQueryWrapper<WorkOrder> closedWrapper = new LambdaQueryWrapper<>();
        closedWrapper.eq(WorkOrder::getOrderStatus, 4);
        data.put("closedCount", workOrderService.count(closedWrapper));

        data.put("totalCount", workOrderService.count());

        return Result.success(data);
    }

    private void fillWorkOrderInfo(List<WorkOrder> orders) {
        orders.forEach(order -> {
            if (order.getForestAreaId() != null) {
                ForestArea area = forestAreaService.getById(order.getForestAreaId());
                if (area != null) order.setForestAreaName(area.getAreaName());
            }
            if (order.getAssignedTo() != null) {
                User user = userService.getById(order.getAssignedTo());
                if (user != null) order.setAssignedToName(user.getRealName());
            }
            if (order.getAssignedBy() != null) {
                User user = userService.getById(order.getAssignedBy());
                if (user != null) order.setAssignedByName(user.getRealName());
            }
        });
    }
}
