package com.tanle.tland.asset_service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "asset")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {
    @Id
    private String id;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private String province;
    private String ward;
    private String address;
    private int[] dimension;
    private Map<String, String> properties;
    private Map<String, String> locationAsset;
    private String[] otherInfo;
    private double landArea;
    private double usableArea;
    private String projectId;
    private String userId;
    private String categoryId;
    @Builder.Default
    private AssetType type = AssetType.DRAFT;
    private List<Content> contents;

    public void addContent(Content content) {
        if (contents == null)
            contents = new ArrayList<>();

        contents.add(content);
    }

    public Image getPoster() {
        Image image = this.getContents().stream()
                .filter(media -> media instanceof Image)
                .map(m -> (Image) m)
                .findAny().get();

        return image;
    }
}
