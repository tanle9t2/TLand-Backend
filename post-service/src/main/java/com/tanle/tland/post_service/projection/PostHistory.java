package com.tanle.tland.post_service.projection;

import com.tanle.tland.post_service.entity.PostStatus;

import java.time.LocalDateTime;

public interface PostHistory {
    String getId();

    String getTitle();

    PostStatus getStatus();

    LocalDateTime getCreatedAt();
}
