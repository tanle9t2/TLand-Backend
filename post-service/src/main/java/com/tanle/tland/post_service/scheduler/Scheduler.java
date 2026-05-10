package com.tanle.tland.post_service.scheduler;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.entity.PostStatus;
import com.tanle.tland.post_service.repo.PostRepo;
import com.tanle.tland.post_service.service.PostService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Scheduler {
    private final PostRepo postRepo;

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
//    @Scheduled(cron = "*/5 * * * * *", zone = "UTC")
    @Transactional
    public void cancelPaymentWaitingAfter48hrs() {
        LocalDateTime endTime = LocalDateTime.now(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay();

        LocalDateTime startTime = endTime.minusDays(2);
        log.info("Scheduler START | startTime={} | endTime={}", startTime, endTime);

        List<Post> posts = postRepo.findAllForCancel(
                PostStatus.WAITING_PAYMENT,
                startTime,
                endTime
        );

        log.info("Found {} posts to cancel", posts.size());
        posts.forEach(p -> p.setStatus(PostStatus.EXPIRED));
        postRepo.saveAll(posts);
        log.info("Scheduler END | cancelled {} posts", posts.size());
    }

}
