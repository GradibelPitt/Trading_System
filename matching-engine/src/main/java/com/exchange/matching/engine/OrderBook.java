package com.exchange.matching.engine;

import com.exchange.common.enums.OrderSide;
import com.exchange.matching.model.OrderBookEntry;
import com.exchange.matching.model.MatchResult;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

/**
 * Single-instrument order book.
 *
 * Bids: TreeMap descending by price  (highest bid first)
 * Asks: TreeMap ascending  by price  (lowest ask first)
 *
 * Each price level holds a PriorityQueue<OrderBookEntry> sorted by entryTime
 * giving strict price-time (FIFO) priority.
 *
 * This class is NOT thread-safe — callers must ensure single-threaded access
 * per instrument (Kafka partition-per-instrument guarantees this).
 */
@Slf4j
public class OrderBook {

    private final String instrument;

    // bids: highest price first
    private final TreeMap<BigDecimal, PriorityQueue<OrderBookEntry>> bids =
            new TreeMap<>(Comparator.reverseOrder());

    // asks: lowest price first
    private final TreeMap<BigDecimal, PriorityQueue<OrderBookEntry>> asks =
            new TreeMap<>();

    // fast lookup for cancel
    private final Map<String, OrderBookEntry> allEntries = new HashMap<>();

    public OrderBook(String instrument) {
        this.instrument = instrument;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public MatchResult matchLimit(OrderBookEntry taker) {
        TreeMap<BigDecimal, PriorityQueue<OrderBookEntry>> oppositeBook =
                taker.getSide() == OrderSide.BUY ? asks : bids;

        List<MatchResult.Fill> fills = new ArrayList<>();
        BigDecimal remaining = taker.getRemainingQty();

        while (remaining.compareTo(BigDecimal.ZERO) > 0 && !oppositeBook.isEmpty()) {
            Map.Entry<BigDecimal, PriorityQueue<OrderBookEntry>> bestLevel =
                    taker.getSide() == OrderSide.BUY
                            ? oppositeBook.firstEntry()
                            : oppositeBook.firstEntry();

            BigDecimal bestPrice = bestLevel.getKey();

            // Price check: limit order only matches at or better than its price
            boolean priceMatch = taker.getSide() == OrderSide.BUY
                    ? taker.getPrice().compareTo(bestPrice) >= 0
                    : taker.getPrice().compareTo(bestPrice) <= 0;

            if (!priceMatch) break;

            remaining = fillAtLevel(taker, bestLevel, fills, remaining, oppositeBook);
        }

        // If there is remaining qty, rest the taker in the book
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            taker.setRemainingQty(remaining);
            addToBook(taker);
        }

        return MatchResult.builder()
                .takerOrderId(taker.getOrderId())
                .fills(fills)
                .remainingQty(remaining)
                .cancelled(false)
                .build();
    }

    public MatchResult matchMarket(OrderBookEntry taker) {
        TreeMap<BigDecimal, PriorityQueue<OrderBookEntry>> oppositeBook =
                taker.getSide() == OrderSide.BUY ? asks : bids;

        List<MatchResult.Fill> fills = new ArrayList<>();
        BigDecimal remaining = taker.getRemainingQty();

        while (remaining.compareTo(BigDecimal.ZERO) > 0 && !oppositeBook.isEmpty()) {
            Map.Entry<BigDecimal, PriorityQueue<OrderBookEntry>> bestLevel =
                    oppositeBook.firstEntry();
            remaining = fillAtLevel(taker, bestLevel, fills, remaining, oppositeBook);
        }

        // Market orders do NOT rest — any unfilled qty is cancelled
        boolean cancelled = remaining.compareTo(BigDecimal.ZERO) > 0;
        if (cancelled) {
            log.warn("Market order {} partially cancelled — insufficient depth, remaining={}",
                    taker.getOrderId(), remaining);
        }

        return MatchResult.builder()
                .takerOrderId(taker.getOrderId())
                .fills(fills)
                .remainingQty(remaining)
                .cancelled(cancelled)
                .build();
    }

    public boolean cancel(String orderId) {
        OrderBookEntry entry = allEntries.remove(orderId);
        if (entry == null) return false;

        TreeMap<BigDecimal, PriorityQueue<OrderBookEntry>> book =
                entry.getSide() == OrderSide.BUY ? bids : asks;

        PriorityQueue<OrderBookEntry> level = book.get(entry.getPrice());
        if (level != null) {
            level.remove(entry);
            if (level.isEmpty()) book.remove(entry.getPrice());
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private BigDecimal fillAtLevel(
            OrderBookEntry taker,
            Map.Entry<BigDecimal, PriorityQueue<OrderBookEntry>> levelEntry,
            List<MatchResult.Fill> fills,
            BigDecimal remaining,
            TreeMap<BigDecimal, PriorityQueue<OrderBookEntry>> oppositeBook) {

        BigDecimal fillPrice = levelEntry.getKey();
        PriorityQueue<OrderBookEntry> queue = levelEntry.getValue();

        while (!queue.isEmpty() && remaining.compareTo(BigDecimal.ZERO) > 0) {
            OrderBookEntry maker = queue.peek();
            BigDecimal fillQty = remaining.min(maker.getRemainingQty());

            fills.add(MatchResult.Fill.builder()
                    .makerOrderId(maker.getOrderId())
                    .makerUserId(maker.getUserId())
                    .takerUserId(taker.getUserId())
                    .price(fillPrice)
                    .quantity(fillQty)
                    .build());

            remaining = remaining.subtract(fillQty);
            maker.setRemainingQty(maker.getRemainingQty().subtract(fillQty));

            if (maker.getRemainingQty().compareTo(BigDecimal.ZERO) == 0) {
                queue.poll();
                allEntries.remove(maker.getOrderId());
            }
        }

        if (queue.isEmpty()) oppositeBook.remove(fillPrice);
        return remaining;
    }

    private void addToBook(OrderBookEntry entry) {
        TreeMap<BigDecimal, PriorityQueue<OrderBookEntry>> book =
                entry.getSide() == OrderSide.BUY ? bids : asks;
        book.computeIfAbsent(entry.getPrice(), p -> new PriorityQueue<>()).add(entry);
        allEntries.put(entry.getOrderId(), entry);
    }

    // -------------------------------------------------------------------------
    // Read-only snapshots (for market-data)
    // -------------------------------------------------------------------------

    public TreeMap<BigDecimal, BigDecimal> getBidDepth(int levels) {
        TreeMap<BigDecimal, BigDecimal> snapshot = new TreeMap<>(Comparator.reverseOrder());
        bids.entrySet().stream().limit(levels).forEach(e ->
                snapshot.put(e.getKey(), e.getValue().stream()
                        .map(OrderBookEntry::getRemainingQty)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));
        return snapshot;
    }

    public TreeMap<BigDecimal, BigDecimal> getAskDepth(int levels) {
        TreeMap<BigDecimal, BigDecimal> snapshot = new TreeMap<>();
        asks.entrySet().stream().limit(levels).forEach(e ->
                snapshot.put(e.getKey(), e.getValue().stream()
                        .map(OrderBookEntry::getRemainingQty)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));
        return snapshot;
    }
}
