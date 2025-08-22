package com.tanle.tland.post_service.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDetailResponse {
    private String id;
    private String title;
    private String description;
    private String createdAt;
    private double price;
    private String type;
    private LocalDateTime lastUpdated;
    private String status;

    private AssetDetailResponse assetDetail;
    private UserPostInfoResponse userInfo;
}
