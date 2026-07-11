package com.forest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forest.dto.PageParam;
import com.forest.dto.Result;
import com.forest.dto.UserRoleDTO;
import com.forest.entity.Role;
import com.forest.entity.User;
import com.forest.mapper.RoleMapper;
import com.forest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(PageParam pageParam) {
        Page<User> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (pageParam.getKeyword() != null && !pageParam.getKeyword().isEmpty()) {
            wrapper.like(User::getUsername, pageParam.getKeyword())
                   .or().like(User::getRealName, pageParam.getKeyword());
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = userService.page(page, wrapper);

        // 填充角色信息
        result.getRecords().forEach(user -> {
            if (user.getRoleId() != null) {
                Role role = roleMapper.selectById(user.getRoleId());
                if (role != null) {
                    user.setRoleName(role.getRoleName());
                    user.setRoleCode(role.getRoleCode());
                }
            }
        });

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    /**
     * 添加用户
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody User user) {
        User existing = userService.getByUsername(user.getUsername());
        if (existing != null) {
            return Result.error("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userService.save(user);
        return Result.success("添加成功", null);
    }

    /**
     * 更新用户
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody User user) {
        user.setUpdatedAt(LocalDateTime.now());
        user.setPassword(null); // 不更新密码
        userService.updateById(user);
        return Result.success("更新成功", null);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        userService.removeById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 分配角色
     */
    @PostMapping("/assign-role")
    public Result<Void> assignRole(@RequestBody UserRoleDTO dto) {
        User user = new User();
        user.setId(dto.getUserId());
        user.setRoleId(dto.getRoleId());
        user.setUpdatedAt(LocalDateTime.now());
        userService.updateById(user);
        return Result.success("角色分配成功", null);
    }

    /**
     * 获取所有护林员（GUARD）列表，供工单指派下拉使用
     */
    @GetMapping("/guards")
    public Result<java.util.List<User>> getGuards() {
        // 先查 GUARD 角色
        Role guardRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, "GUARD"));
        if (guardRole == null) {
            return Result.success(java.util.Collections.emptyList());
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRoleId, guardRole.getId())
               .eq(User::getStatus, 1);
        java.util.List<User> guards = userService.list(wrapper);
        guards.forEach(u -> {
            u.setPassword(null);
            u.setRoleName(guardRole.getRoleName());
            u.setRoleCode(guardRole.getRoleCode());
        });
        return Result.success(guards);
    }

    /**
     * 获取用户信息
     * 支持通过参数id获取指定用户，或从token获取当前用户
     */
    @GetMapping("/info")
    public Result<User> getInfo(@RequestParam(required = false) Integer id,
                                @RequestAttribute(value = "userId", required = false) Integer tokenUserId) {
        Integer userId = (id != null) ? id : tokenUserId;
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 填充角色信息
        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                user.setRoleName(role.getRoleName());
                user.setRoleCode(role.getRoleCode());
            }
        }
        // 清空密码
        user.setPassword(null);
        return Result.success(user);
    }
}
