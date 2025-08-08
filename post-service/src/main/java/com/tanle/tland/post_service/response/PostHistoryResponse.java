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
public class PostHistoryResponse {
    private String id;
    private String title;
    private String status;
    private LocalDateTime createdAt;

}
