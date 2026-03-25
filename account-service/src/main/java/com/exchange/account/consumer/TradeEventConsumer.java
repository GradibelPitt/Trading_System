package com.exchange.account.consumer;

import com.exchange.account.service.AccountService;
import com.exchange.common.enums.OrderSide;
import com.exchange.common.event.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consumes trade-events from Kafka and settles buyer/seller balances.
 *
 * Instrument format assumed: BASE-QUOTE  e.g. "BTC-USDT"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final AccountService accountService;

    @KafkaListener(topics = "trade-events", groupId = "account-service")
    public void onTradeEvent(TradeEvent event) {
        log.info("Settling trade {}: {} {} @ {}",
                event.getTradeId(), event.getQuantity(), event.getInstrument(), event.getPrice());

        String[] parts = event.getInstrument().split("-");
        if (parts.length != 2) {
            log.error("Invalid instrument format: {}", event.getInstrument());
            return;
        }
        String baseAsset  = parts[0];   // e.g. BTC
        String quoteAsset = parts[1];   // e.g. USDT

        BigDecimal quoteAmount = event.getPrice().multiply(event.getQuantity());

        // Taker side determines buyer/seller roles
        String buyerUserId  = event.getTakerSide() == OrderSide.BUY
                ? event.getTakerUserId() : event.getMakerUserId();
        String sellerUserId = event.getTakerSide() == OrderSide.BUY
                ? event.getMakerUserId() : event.getTakerUserId();

        try {
            accountService.settleTrade(
                    buyerUserId, sellerUserId,
                    baseAsset, quoteAsset,
                    event.getQuantity(), quoteAmount);
        } catch (Exception ex) {
            log.error("Settlement failed for trade {}: {}", event.getTradeId(), ex.getMessage(), ex);
            // In production: publish to a dead-letter topic for manual reconciliation
        }
    }
}
