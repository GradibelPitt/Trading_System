package com.exchange.order.dto;

import com.exchange.common.enums.OrderSide;
import com.exchange.common.enums.OrderType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceOrderRequest {

    @NotBlank
    private String instrument;

    @NotNull
    private OrderSide side;

    @NotNull
    private OrderType type;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal price;    // required for LIMIT, null for MARKET

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal quantity;
}
