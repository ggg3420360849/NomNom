package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.UserEntity;
import com.nomnom.backend.mapper.UserMapper;
import com.nomnom.backend.model.AuthUser;
import com.nomnom.backend.model.LoginRequest;
import com.nomnom.backend.model.RegisterRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final int ROLE_CUSTOMER = 1;
    private static final int ROLE_MERCHANT = 2;

    private final UserMapper userMapper;

    public AuthService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public AuthUser login(LoginRequest request) {
        validateLoginRequest(request);

        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getAccount, request.getAccount().trim())
                .last("LIMIT 1"));

        if (user == null) {
            throw new IllegalArgumentException("账号不存在。");
        }
        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("密码错误。");
        }

        int expectedRole = resolveRoleValue(request.getRoleCode());
        if (user.getRole() == null || user.getRole() != expectedRole) {
            throw new IllegalArgumentException("账号角色与当前入口不匹配。");
        }

        return toAuthUser(user);
    }

    @Transactional
    public AuthUser register(RegisterRequest request) {
        validateRegisterRequest(request);

        UserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getAccount, request.getAccount().trim())
                .last("LIMIT 1"));
        if (existing != null) {
            throw new IllegalArgumentException("该账号已存在，请直接登录。");
        }

        UserEntity user = new UserEntity();
        user.setAccount(request.getAccount().trim());
        user.setUsername(resolveUsername(request));
        user.setPassword(request.getPassword());
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setRole(resolveRoleValue(request.getRoleCode()));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        return toAuthUser(user);
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request.getAccount() == null || request.getAccount().trim().isEmpty()) {
            throw new IllegalArgumentException("请输入账号。");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("请输入密码。");
        }
        resolveRoleValue(request.getRoleCode());
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.getAccount() == null || request.getAccount().trim().isEmpty()) {
            throw new IllegalArgumentException("请输入账号。");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位。");
        }
        resolveRoleValue(request.getRoleCode());
    }

    private String resolveUsername(RegisterRequest request) {
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            return request.getUsername().trim();
        }
        return request.getAccount().trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private int resolveRoleValue(String roleCode) {
        if ("CUSTOMER".equalsIgnoreCase(roleCode)) {
            return ROLE_CUSTOMER;
        }
        if ("MERCHANT".equalsIgnoreCase(roleCode)) {
            return ROLE_MERCHANT;
        }
        throw new IllegalArgumentException("角色参数非法。");
    }

    private AuthUser toAuthUser(UserEntity user) {
        AuthUser authUser = new AuthUser();
        authUser.setId(user.getId());
        authUser.setAccount(user.getAccount());
        authUser.setUsername(user.getUsername());
        authUser.setRole(user.getRole());
        authUser.setRoleCode(user.getRole() != null && user.getRole() == ROLE_MERCHANT ? "MERCHANT" : "CUSTOMER");
        authUser.setRoleLabel(user.getRole() != null && user.getRole() == ROLE_MERCHANT ? "商家端" : "顾客端");
        authUser.setEmail(user.getEmail());
        authUser.setPhone(user.getPhone());
        return authUser;
    }
}
