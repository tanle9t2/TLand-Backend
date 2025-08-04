package com.tanle.tland.post_service.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostOverviewResponse {
    private String id;
    private String title;
    private double price;
    private LocalDateTime createdAt;
    private String posterUrl;
}
