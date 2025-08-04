package com.tanle.tland.post_service.projection;

import java.time.LocalDateTime;

public interface PostOverview {
    String getId();

    String getTitle();

    LocalDateTime getCreatedAt();

    double getPrice();

    String getAssetId();
}
