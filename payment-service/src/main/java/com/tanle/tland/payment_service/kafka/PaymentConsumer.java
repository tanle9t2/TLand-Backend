package com.tanle.tland.payment_service.kafka;

import com.tanle.tland.payment_service.event.OrderEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {

    @KafkaListener(topics = "order-topic", groupId = "saga-service")
    public void consumeEventAndPublisheTopic(List<OrderEvent> orderEvents) {
        System.out.println("consume");
//        Flux.fromIterable(orderEvents)
//                .flatMap(this::processPayment)
//                .subscribe();
    }
//    private Mono<PaymentEvent> processPayment(OrderEvent orderEvent) {
//        if (OrderStatus.ORDER_CREATED.equals(orderEvent.getOrderStatus())) {
//            return Mono.fromSupplier(() -> paymentService.newOrderEvent(orderEvent));
//        } else {
//            return Mono.fromRunnable(() -> paymentService.cancelOrderEvent(orderEvent));
//        }
//    }
}
