package com.exchange.order.controller;

import com.exchange.common.dto.ApiResponse;
import com.exchange.common.enums.OrderStatus;
import com.exchange.order.dto.OrderResponse;
import com.exchange.order.dto.PlaceOrderRequest;
import com.exchange.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> placeOrder(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PlaceOrderRequest req) {
        return ApiResponse.ok(orderService.placeOrder(userId, req), "Order accepted");
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<OrderResponse> cancelOrder(
            @AuthenticationPrincipal String userId,
            @PathVariable String orderId) {
        return ApiResponse.ok(orderService.cancelOrder(userId, orderId));
    }

    @GetMapping
    public ApiResponse<Page<OrderResponse>> listOrders(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(orderService.getOrders(userId, status, pageable));
    }
}
