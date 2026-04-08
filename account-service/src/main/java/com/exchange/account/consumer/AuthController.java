package com.exchange.account.controller;

import com.exchange.account.dto.AuthDto;
import com.exchange.account.service.AuthService;
import com.exchange.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthDto.AuthResponse> register(
            @Valid @RequestBody AuthDto.RegisterRequest req) {
        return ApiResponse.ok(authService.register(req), "Registration successful");
    }

    @PostMapping("/login")
    public ApiResponse<AuthDto.AuthResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }
}
