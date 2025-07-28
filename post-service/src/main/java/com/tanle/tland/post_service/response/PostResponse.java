package com.tanle.tland.post_service.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PostResponse {
    private String id;
    private String title;
    private String description;
    private String createdAt;
    private String type;
    private LocalDateTime lastUpdated;
    private String userId;
    private String status;
    private AssetDetailResponse assetDetail;

}
