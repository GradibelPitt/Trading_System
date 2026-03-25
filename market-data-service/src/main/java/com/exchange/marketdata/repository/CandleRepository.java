package com.exchange.marketdata.repository;

import com.exchange.marketdata.entity.Candle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CandleRepository extends JpaRepository<Candle, Long> {

    List<Candle> findByInstrumentAndIntervalTypeAndOpenTimeBetweenOrderByOpenTimeAsc(
            String instrument, String intervalType, Instant from, Instant to);

    Optional<Candle> findByInstrumentAndIntervalTypeAndOpenTime(
            String instrument, String intervalType, Instant openTime);

    @Query("""
            SELECT c FROM Candle c
            WHERE c.instrument = :instrument
              AND c.intervalType = :interval
            ORDER BY c.openTime DESC
            LIMIT :limit
            """)
    List<Candle> findLatest(
            @Param("instrument") String instrument,
            @Param("interval") String interval,
            @Param("limit") int limit);
}
