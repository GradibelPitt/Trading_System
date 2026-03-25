package com.exchange.order.service;

import com.exchange.common.enums.OrderStatus;
import com.exchange.common.event.OrderEvent;
import com.exchange.order.dto.PlaceOrderRequest;
import com.exchange.order.dto.OrderResponse;
import com.exchange.order.entity.Order;
import com.exchange.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @Transactional
    public OrderResponse placeOrder(String userId, PlaceOrderRequest req) {
        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .instrument(req.getInstrument())
                .side(req.getSide())
                .type(req.getType())
                .price(req.getPrice())
                .quantity(req.getQuantity())
                .status(OrderStatus.PENDING)
                .build();

        orderRepository.save(order);
        log.info("Order saved: {}", order.getId());

        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .userId(userId)
                .instrument(order.getInstrument())
                .side(order.getSide())
                .type(order.getType())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .action(OrderEvent.Action.PLACE)
                .eventTime(Instant.now())
                .build();

        kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId(), event);
        log.info("OrderEvent published: {}", order.getId());

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new com.exchange.common.exception.ExchangeException(
                        "ORDER_NOT_FOUND", "Order not found: " + orderId));

        if (!List.of(OrderStatus.PENDING, OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)
                .contains(order.getStatus())) {
            throw new com.exchange.common.exception.ExchangeException(
                    "ORDER_NOT_CANCELLABLE", "Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .userId(userId)
                .instrument(order.getInstrument())
                .action(OrderEvent.Action.CANCEL)
                .eventTime(Instant.now())
                .build();

        kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId(), event);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(String userId, OrderStatus status, Pageable pageable) {
        Page<Order> page = (status != null)
                ? orderRepository.findByUserIdAndStatus(userId, status, pageable)
                : orderRepository.findByUserId(userId, pageable);
        return page.map(OrderResponse::from);
    }
}
