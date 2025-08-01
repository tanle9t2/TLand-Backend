package com.tanle.tland.asset_service.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSummaryResponse {
    private String id;
    private String address;
    private String ward;
    private String province;
    private double landArea;
    private double usableArea;
    private LocalDateTime createdAt;
    private String imageUrl;
    private double totalImages;
}
