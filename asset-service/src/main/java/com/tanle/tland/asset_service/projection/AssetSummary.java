package com.tanle.tland.asset_service.projection;

import com.tanle.tland.asset_service.entity.Content;
import com.tanle.tland.asset_service.entity.Image;

import java.time.LocalDateTime;
import java.util.List;

public interface AssetSummary {
    String getId();

    String getAddress();

    String getWard();

    String getProvince();

    double getLandArea();

    double getUsableArea();

    LocalDateTime getCreatedAt();

    List<Content> getContents();
}
