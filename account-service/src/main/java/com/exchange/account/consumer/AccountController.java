package com.exchange.account.controller;

import com.exchange.account.dto.BalanceResponse;
import com.exchange.account.service.AccountService;
import com.exchange.common.dto.ApiResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/balances")
    public ApiResponse<List<BalanceResponse>> getBalances(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(accountService.getBalances(userId));
    }

    @GetMapping("/balances/{asset}")
    public ApiResponse<BalanceResponse> getBalance(
            @AuthenticationPrincipal String userId,
            @PathVariable String asset) {
        return ApiResponse.ok(accountService.getBalance(userId, asset.toUpperCase()));
    }

    /** MVP deposit — seed test funds. */
    @PostMapping("/deposit")
    public ApiResponse<BalanceResponse> deposit(
            @AuthenticationPrincipal String userId,
            @RequestBody DepositRequest req) {
        return ApiResponse.ok(
                accountService.deposit(userId, req.getAsset().toUpperCase(), req.getAmount()));
    }

    @Data
    static class DepositRequest {
        @NotBlank private String asset;
        @DecimalMin("0.0001") private BigDecimal amount;
    }
}
