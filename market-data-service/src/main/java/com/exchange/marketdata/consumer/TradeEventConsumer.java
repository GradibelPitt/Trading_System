package com.exchange.marketdata.consumer;

import com.exchange.common.event.TradeEvent;
import com.exchange.marketdata.dto.TickerMessage;
import com.exchange.marketdata.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final CandleService candleService;

    @KafkaListener(
            topics = "${exchange.kafka.topics.trade-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onTradeEvent(TradeEvent event) {
        log.debug("Market data received trade {} {} @ {}",
                event.getInstrument(), event.getQuantity(), event.getPrice());

        // 1. Broadcast real-time ticker to WebSocket subscribers
        TickerMessage ticker = TickerMessage.builder()
                .instrument(event.getInstrument())
                .lastPrice(event.getPrice())
                .lastQty(event.getQuantity())
                .tradeTime(event.getTradeTime())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/ticker." + event.getInstrument(), ticker);

        // 2. Update K-line aggregations
        candleService.onTrade(event);
    }
}
