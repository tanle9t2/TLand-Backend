package com.tanle.tland.asset_service.response;

import com.tanle.tland.asset_service.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import javax.print.attribute.standard.Media;
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
    private boolean isAttachedPostShow;
    private LocalDateTime createdAt;
    private String province;
    private String ward;
    private String address;
    private int[] dimension;
    private String[] otherInfo;
    private Map<String, String> locationAsset;
    private Map<String, String> properties;
    private double landArea;
    private double usableArea;
    private String projectId;
    private String userId;
    private List<Content> contents;
    private CategoryResponse category;


    public static class Media {
        private String id;
        private String url;
        private String name;
        private LocalDateTime createdAt;
    }
}
