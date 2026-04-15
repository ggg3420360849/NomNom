package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.model.AuthUser;
import com.nomnom.backend.model.LoginRequest;
import com.nomnom.backend.model.RegisterRequest;
import com.nomnom.backend.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthUser> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok("login success", authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<AuthUser> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok("register success", authService.register(request));
    }
}
