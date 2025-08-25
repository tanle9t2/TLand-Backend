package com.tanle.tland.search_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanle.tland.search_service.service.AsyncService;
import com.tanle.tland.search_service.utils.KafkaOperator;
import jakarta.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchConsumer {
    private final AsyncService asyncService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "tland.tland_postdb.post",
            groupId = "tland-async"
    )
    public void syncCourse(ConsumerRecord<?, ?> consumerRecord) {
        try {
            JsonNode root = objectMapper.readTree((String) consumerRecord.value());
            JsonNode payload = root.path("payload");

            if (payload.isMissingNode()) {
                log.warn("No payload found in message: {}", consumerRecord.value());
                return;
            }

            String op = payload.path("op").asText();
            String postId = null;

            switch (op) {
                case "c": // CREATE
                    postId = payload.path("after").path("id").asText();
                    asyncService.createPost(postId);
                    break;

                case "u": // UPDATE
                    postId = payload.path("after").path("id").asText();
                    asyncService.updatePost(postId);
                    break;

                case "d": // DELETE
                    postId = payload.path("before").path("id").asText();
                    asyncService.deletePost(postId);
                    break;

                default:
                    log.warn("Unsupported op: {}", op);
            }
        } catch (Exception e) {
            log.error("Failed to process message: {}", consumerRecord.value(), e);
        }
    }
}
