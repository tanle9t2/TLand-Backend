package com.tanle.tland.search_service.entity;

import com.tanle.tland.search_service.entity.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDocument {
    @Id
    private String id;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Text)
    private String province;

    @Field(type = FieldType.Text)
    private String ward;

    @Field(type = FieldType.Text)
    private String address;

    @Field(type = FieldType.Integer)
    private int[] dimension;

    @Field(type = FieldType.Object)
    private Map<String, String> properties;

    @Field(type = FieldType.Object)
    private Map<String, String> locationAsset;

    @Field(type = FieldType.Text)
    private String[] otherInfo;

    @Field(type = FieldType.Double)
    private double landArea;

    @Field(type = FieldType.Double)
    private double usableArea;

    @Field(type = FieldType.Text)
    private String projectId;

    @Field(type = FieldType.Text)
    private String userId;

    @Field(type = FieldType.Object)
    private CategoryDocument category;

    @Field(type = FieldType.Keyword)
    private AssetType type;

    @Field(type = FieldType.Object)
    private MediaDocument media;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaDocument {
        private String id;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDocument {
        public String id;
        private String name;
    }
}
