package com.forest.controller;

import com.forest.dto.LoginDTO;
import com.forest.dto.PasswordDTO;
import com.forest.dto.Result;
import com.forest.entity.Role;
import com.forest.entity.User;
import com.forest.mapper.RoleMapper;
import com.forest.service.UserService;
import com.forest.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleMapper roleMapper;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        User user = userService.getByUsername(loginDTO.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.error("密码错误");
        }

        // 获取角色信息
        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                user.setRoleName(role.getRoleName());
                user.setRoleCode(role.getRoleCode());
            }
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleCode());
        userService.updateLoginInfo(user.getId(), loginDTO.getIp());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("roleCode", user.getRoleCode());
        data.put("roleName", user.getRoleName());

        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功", null);
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody PasswordDTO dto,
                                       @RequestAttribute("userId") Integer userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.error("原密码错误");
        }
        if (dto.getNewPassword() != null && dto.getConfirmPassword() != null
                && !dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.error("两次输入的密码不一致");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.updateById(update);
        return Result.success("密码修改成功", null);
    }

    /**
     * 用户注册
     * 参数：username, password, realName, phone, roleCode（可选GUARD/MANAGER），默认status=1
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String realName = (String) params.get("realName");
        String phone = (String) params.get("phone");
        String roleCode = params.get("roleCode") != null ? (String) params.get("roleCode") : "GUARD";

        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (realName == null || realName.trim().isEmpty()) {
            return Result.error("真实姓名不能为空");
        }

        // 检查用户名是否已存在
        User existing = userService.getByUsername(username);
        if (existing != null) {
            return Result.error("用户名已存在");
        }

        // 根据roleCode查找角色
        if (!"GUARD".equals(roleCode) && !"MANAGER".equals(roleCode)) {
            roleCode = "GUARD"; // 默认为GUARD
        }
        Role role = roleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleCode, roleCode));
        if (role == null) {
            return Result.error("角色不存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRoleId(role.getId());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userService.save(user);

        return Result.success("注册成功", null);
    }
}
