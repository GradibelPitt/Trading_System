package com.exchange.marketdata.service;

import com.exchange.common.event.TradeEvent;
import com.exchange.marketdata.entity.Candle;
import com.exchange.marketdata.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleService {

    private final CandleRepository candleRepository;

    private static final List<String> INTERVALS = List.of("1m", "5m", "1h");

    @Transactional
    public void onTrade(TradeEvent trade) {
        for (String interval : INTERVALS) {
            Instant openTime = floorToInterval(trade.getTradeTime(), interval);
            Instant closeTime = openTime.plus(intervalMillis(interval), ChronoUnit.MILLIS)
                    .minusMillis(1);

            Candle candle = candleRepository
                    .findByInstrumentAndIntervalTypeAndOpenTime(
                            trade.getInstrument(), interval, openTime)
                    .orElseGet(() -> Candle.builder()
                            .instrument(trade.getInstrument())
                            .intervalType(interval)
                            .openTime(openTime)
                            .closeTime(closeTime)
                            .open(trade.getPrice())
                            .high(trade.getPrice())
                            .low(trade.getPrice())
                            .close(trade.getPrice())
                            .volume(trade.getQuantity())
                            .tradeCount(0L)
                            .build());

            // Update OHLCV
            if (trade.getPrice().compareTo(candle.getHigh()) > 0)
                candle.setHigh(trade.getPrice());
            if (trade.getPrice().compareTo(candle.getLow()) < 0)
                candle.setLow(trade.getPrice());
            candle.setClose(trade.getPrice());
            candle.setVolume(candle.getVolume().add(trade.getQuantity()));
            candle.setTradeCount(candle.getTradeCount() + 1);

            candleRepository.save(candle);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Instant floorToInterval(Instant t, String interval) {
        long epochMs = t.toEpochMilli();
        long ms = intervalMillis(interval);
        return Instant.ofEpochMilli((epochMs / ms) * ms);
    }

    private long intervalMillis(String interval) {
        return switch (interval) {
            case "1m" -> 60_000L;
            case "5m" -> 300_000L;
            case "1h" -> 3_600_000L;
            default   -> throw new IllegalArgumentException("Unknown interval: " + interval);
        };
    }
}
