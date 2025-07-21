package com.tanle.tland.post_service.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PostCreateRequest {

    private String title;
    private String description;
    private LocalDateTime lastUpdated;
    private String assetId;
    private String userId;
}
