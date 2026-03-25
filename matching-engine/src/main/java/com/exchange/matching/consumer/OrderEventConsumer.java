package com.exchange.matching.consumer;

import com.exchange.common.event.OrderEvent;
import com.exchange.matching.engine.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final MatchingService matchingService;

    /**
     * One partition per instrument ensures sequential processing per instrument.
     * concurrency should match the number of Kafka partitions on order-events topic.
     */
    @KafkaListener(
            topics = "${exchange.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "4"
    )
    public void onOrderEvent(
            OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        log.debug("Received OrderEvent partition={} orderId={} action={}",
                partition, event.getOrderId(), event.getAction());
        try {
            matchingService.process(event);
        } catch (Exception ex) {
            log.error("Failed to process OrderEvent orderId={}", event.getOrderId(), ex);
            // TODO: send to dead-letter topic
        }
    }
}
