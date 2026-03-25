package com.exchange.marketdata.controller;

import com.exchange.common.dto.ApiResponse;
import com.exchange.marketdata.entity.Candle;
import com.exchange.marketdata.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final CandleRepository candleRepository;

    /**
     * GET /api/v1/market-data/candles?instrument=BTC-USDT&interval=1m&from=...&to=...
     */
    @GetMapping("/candles")
    public ApiResponse<List<Candle>> getCandles(
            @RequestParam String instrument,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        List<Candle> candles = candleRepository
                .findByInstrumentAndIntervalTypeAndOpenTimeBetweenOrderByOpenTimeAsc(
                        instrument, interval, from, to);
        return ApiResponse.ok(candles);
    }

    /**
     * GET /api/v1/market-data/candles/latest?instrument=BTC-USDT&interval=1m&limit=100
     */
    @GetMapping("/candles/latest")
    public ApiResponse<List<Candle>> getLatestCandles(
            @RequestParam String instrument,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "200") int limit) {

        List<Candle> candles = candleRepository.findLatest(instrument, interval,
                Math.min(limit, 1000));
        return ApiResponse.ok(candles);
    }
}
