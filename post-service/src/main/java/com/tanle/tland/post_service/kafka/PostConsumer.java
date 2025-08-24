package com.tanle.tland.post_service.kafka;

import com.tanle.tland.post_service.entity.PostStatus;
import com.tanle.tland.post_service.entity.PurposeType;
import com.tanle.tland.post_service.event.PaymentEvent;
import com.tanle.tland.post_service.event.PaymentStatus;
import com.tanle.tland.post_service.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostConsumer {
    private final PostService postService;
    private final String PAYMENT_TOPIC = "tland-payment-topic";

    @KafkaListener(topics = PAYMENT_TOPIC, groupId = "tland-post")
    public void consumeTopic(List<PaymentEvent> paymentEvents) {
        System.out.println("consume");
        Flux.fromIterable(paymentEvents)
                .filter(paymentEvent -> PurposeType.POST.name().equals(paymentEvent.getPaymentRequestDto().getPurposeType()))
                .flatMap(paymentEvent -> Mono.fromRunnable(() -> {

                    if (PaymentStatus.SUCCESS.equals(paymentEvent.getPaymentStatus()))
                        postService.updateStatusPost(paymentEvent.getPaymentRequestDto().getPurposeId(),
                                PostStatus.WAITING_ACCEPT);
                    else
                        postService.deletePost(paymentEvent.getPaymentRequestDto().getPurposeId());

                }))
                .subscribe();
    }

}
