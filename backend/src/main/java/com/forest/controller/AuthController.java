package com.forest.controller;

import com.forest.dto.LoginDTO;
import com.forest.dto.Result;
import com.forest.entity.User;
import com.forest.service.UserService;
import com.forest.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleCode());
        userService.updateLoginInfo(user.getId(), loginDTO.getIp());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("roleCode", user.getRoleCode());

        return Result.success(data);
    }
}