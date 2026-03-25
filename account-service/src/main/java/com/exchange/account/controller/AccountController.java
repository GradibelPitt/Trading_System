package com.exchange.account.controller;

import com.exchange.account.dto.BalanceResponse;
import com.exchange.account.service.AccountService;
import com.exchange.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/balances")
    public ApiResponse<List<BalanceResponse>> getAllBalances(
            @RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(accountService.getBalances(userId));
    }

    @GetMapping("/balances/{asset}")
    public ApiResponse<BalanceResponse> getBalance(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String asset) {
        return ApiResponse.ok(accountService.getBalance(userId, asset.toUpperCase()));
    }
}
