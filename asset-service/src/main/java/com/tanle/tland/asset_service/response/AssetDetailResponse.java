package com.tanle.tland.asset_service.response;

import com.tanle.tland.asset_service.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AssetDetailResponse {
    private String id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private String province;
    private String ward;
    private String address;
    private int[] dimension;
    private Map<String, String> properties;
    private String projectId;
    private String userId;
    private List<Content> contents;
}
