package com.exchange.marketdata.service;

import com.exchange.marketdata.dto.OrderBookSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a local mirror of each instrument's order book depth,
 * updated by trade events, and broadcasts L2 snapshots every 500ms.
 *
 * For MVP the depth mirror is populated from trade prices only
 * (no full book sync). Phase 5 full impl will consume OrderBook
 * snapshots directly from matching-engine via a dedicated Kafka topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBookBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    // instrument -> { price -> qty }  (simple running bid/ask approximation)
    private final Map<String, TreeMap<BigDecimal, BigDecimal>> bidMirror = new ConcurrentHashMap<>();
    private final Map<String, TreeMap<BigDecimal, BigDecimal>> askMirror = new ConcurrentHashMap<>();

    private static final int DEPTH_LEVELS = 10;

    /** Called by TradeEventConsumer on each fill. */
    public void onFill(String instrument, BigDecimal price, BigDecimal qty) {
        // Remove filled qty from both sides at that price level (simplified)
        bidMirror.computeIfAbsent(instrument, k -> new TreeMap<>(Comparator.reverseOrder()))
                .merge(price, qty.negate(), BigDecimal::add);
        askMirror.computeIfAbsent(instrument, k -> new TreeMap<>())
                .merge(price, qty.negate(), BigDecimal::add);

        // Clean up zero/negative levels
        bidMirror.get(instrument).entrySet().removeIf(e -> e.getValue().compareTo(BigDecimal.ZERO) <= 0);
        askMirror.get(instrument).entrySet().removeIf(e -> e.getValue().compareTo(BigDecimal.ZERO) <= 0);
    }

    @Scheduled(fixedDelay = 500)
    public void broadcastSnapshots() {
        Set<String> instruments = new HashSet<>();
        instruments.addAll(bidMirror.keySet());
        instruments.addAll(askMirror.keySet());

        for (String instrument : instruments) {
            OrderBookSnapshot snapshot = buildSnapshot(instrument);
            messagingTemplate.convertAndSend("/topic/depth." + instrument, snapshot);
        }
    }

    private OrderBookSnapshot buildSnapshot(String instrument) {
        List<OrderBookSnapshot.PriceLevel> bids = bidMirror
                .getOrDefault(instrument, new TreeMap<>(Comparator.reverseOrder()))
                .entrySet().stream()
                .limit(DEPTH_LEVELS)
                .map(e -> OrderBookSnapshot.PriceLevel.builder()
                        .price(e.getKey()).qty(e.getValue()).build())
                .toList();

        List<OrderBookSnapshot.PriceLevel> asks = askMirror
                .getOrDefault(instrument, new TreeMap<>())
                .entrySet().stream()
                .limit(DEPTH_LEVELS)
                .map(e -> OrderBookSnapshot.PriceLevel.builder()
                        .price(e.getKey()).qty(e.getValue()).build())
                .toList();

        return OrderBookSnapshot.builder()
                .instrument(instrument)
                .bids(bids)
                .asks(asks)
                .snapshotTime(Instant.now())
                .build();
    }
}
