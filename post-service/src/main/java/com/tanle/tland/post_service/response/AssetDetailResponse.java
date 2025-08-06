package com.tanle.tland.post_service.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private double price;
    private LocalDateTime createdAt;
    private double landArea;
    private double usableArea;
    private String province;
    private String ward;
    private String address;
    private List<Integer> dimension;
    private Map<String, String> properties;
    private String projectId;
    private String userId;
    private List<Content> contents;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class Content {
        private String id;
        private String name;
        private String url;
        private String type;
        private double duration;
    }

}