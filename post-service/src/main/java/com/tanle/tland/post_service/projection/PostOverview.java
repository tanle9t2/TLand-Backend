package com.tanle.tland.post_service.projection;

import com.tanle.tland.post_service.entity.PostStatus;
import com.tanle.tland.post_service.entity.PostType;

import java.time.LocalDateTime;

public interface PostOverview {
    String getId();

    String getTitle();

    LocalDateTime getCreatedAt();

    double getPrice();

    String getAssetId();

    String getUserId();

    PostStatus getStatus();
    PostType getType();
}
