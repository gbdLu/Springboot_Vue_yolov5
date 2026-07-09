package com.forest.config;

import com.forest.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false;
        }

        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效\"}");
            return false;
        }

        request.setAttribute("userId", jwtUtil.getUserId(token));
        request.setAttribute("roleCode", jwtUtil.getRoleCode(token));
        return true;
    }
}