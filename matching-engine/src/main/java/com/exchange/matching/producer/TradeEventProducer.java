package com.exchange.matching.producer;

import com.exchange.common.event.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventProducer {

    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;

    @Value("${exchange.kafka.topics.trade-events}")
    private String tradeEventsTopic;

    public void publish(TradeEvent event) {
        // Key by instrument so market-data consumers get ordered fills per symbol
        kafkaTemplate.send(tradeEventsTopic, event.getInstrument(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TradeEvent tradeId={}", event.getTradeId(), ex);
                    }
                });
    }
}
