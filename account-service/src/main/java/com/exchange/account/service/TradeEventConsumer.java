package com.exchange.account.service;

import com.exchange.common.event.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final BalanceService balanceService;

    @KafkaListener(
            topics = "${exchange.kafka.topics.trade-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onTradeEvent(TradeEvent event) {
        log.debug("Settling trade {}", event.getTradeId());
        try {
            balanceService.settle(event);
        } catch (Exception ex) {
            log.error("Settlement failed for trade {}", event.getTradeId(), ex);
            // TODO: dead-letter + manual reconciliation
        }
    }
}
