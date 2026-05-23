package com.nomnom.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.common.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeJson(response, HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期");
            return false;
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            writeJson(response, HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期");
            return false;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/api/admin/")) {
            String roleCode = jwtUtils.getRoleCode(token);
            if (!"MERCHANT".equals(roleCode)) {
                writeJson(response, HttpStatus.FORBIDDEN.value(), "当前账号无权访问商家端");
                return false;
            }
        }

        request.setAttribute("userId", jwtUtils.getUserId(token));
        request.setAttribute("role", jwtUtils.getRole(token));
        request.setAttribute("roleCode", jwtUtils.getRoleCode(token));
        return true;
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(message)));
    }
}
