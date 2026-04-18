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
import java.util.Arrays;
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

    @Override
    public String toString() {
        return "Asset{" +
                "id='" + id + '\'' +
                ", createdAt=" + createdAt +
                ", province='" + province + '\'' +
                ", ward='" + ward + '\'' +
                ", address='" + address + '\'' +
                ", dimension=" + Arrays.toString(dimension) +
                ", properties=" + properties +
                ", locationAsset=" + locationAsset +
                ", otherInfo=" + Arrays.toString(otherInfo) +
                ", landArea=" + landArea +
                ", usableArea=" + usableArea +
                ", projectId='" + projectId + '\'' +
                ", userId='" + userId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", type=" + type +
                ", contents=" + contents +
                '}';
    }

    public Image getPoster() {
        Image image = this.getContents().stream()
                .filter(media -> media instanceof Image)
                .map(m -> (Image) m)
                .findAny().get();

        return image;
    }
}
