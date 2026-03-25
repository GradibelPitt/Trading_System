package com.exchange.matching.engine;

import com.exchange.common.enums.OrderType;
import com.exchange.common.event.OrderEvent;
import com.exchange.common.event.TradeEvent;
import com.exchange.matching.model.OrderBookEntry;
import com.exchange.matching.model.MatchResult;
import com.exchange.matching.producer.TradeEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final TradeEventProducer tradeEventProducer;

    // One OrderBook per instrument — created on first order
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();

    public void process(OrderEvent event) {
        if (event.getAction() == OrderEvent.Action.CANCEL) {
            handleCancel(event);
            return;
        }

        OrderBook book = books.computeIfAbsent(event.getInstrument(), OrderBook::new);
        OrderBookEntry taker = toEntry(event);

        MatchResult result = event.getType() == OrderType.MARKET
                ? book.matchMarket(taker)
                : book.matchLimit(taker);

        publishFills(event, result);
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void handleCancel(OrderEvent event) {
        OrderBook book = books.get(event.getInstrument());
        if (book == null) return;
        boolean removed = book.cancel(event.getOrderId());
        log.info("Cancel order {}: {}", event.getOrderId(), removed ? "removed" : "not found");
    }

    private void publishFills(OrderEvent event, MatchResult result) {
        if (result.getFills().isEmpty()) return;

        for (MatchResult.Fill fill : result.getFills()) {
            TradeEvent trade = TradeEvent.builder()
                    .tradeId(UUID.randomUUID().toString())
                    .instrument(event.getInstrument())
                    .makerOrderId(fill.getMakerOrderId())
                    .takerOrderId(result.getTakerOrderId())
                    .makerUserId(fill.getMakerUserId())
                    .takerUserId(fill.getTakerUserId())
                    .takerSide(event.getSide())
                    .price(fill.getPrice())
                    .quantity(fill.getQuantity())
                    .makerFee(fill.getQuantity().multiply(fill.getPrice())
                            .multiply(new BigDecimal("0.001")))  // 0.1% maker fee
                    .takerFee(fill.getQuantity().multiply(fill.getPrice())
                            .multiply(new BigDecimal("0.002")))  // 0.2% taker fee
                    .tradeTime(Instant.now())
                    .build();

            tradeEventProducer.publish(trade);
            log.info("Trade: {} {} @ {} qty={}", event.getInstrument(),
                    event.getSide(), fill.getPrice(), fill.getQuantity());
        }
    }

    private OrderBookEntry toEntry(OrderEvent event) {
        return OrderBookEntry.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .instrument(event.getInstrument())
                .side(event.getSide())
                .price(event.getPrice())
                .remainingQty(event.getQuantity())
                .entryTime(event.getEventTime() != null ? event.getEventTime() : Instant.now())
                .build();
    }
}
