package com.tanle.tland.payment_service.kafka;

import com.tanle.tland.payment_service.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentPublisher {
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final String TOPIC = "tland-payment-topic";

    public void publishMessage(PaymentEvent paymentEvent) {
        kafkaTemplate.send(TOPIC, String.valueOf(paymentEvent.getPaymentRequestDto().getPurposeId()), paymentEvent);
        System.out.println("Payment event published: " + paymentEvent);
    }
}
