package com.tanle.tland.search_service.entity;

import com.tanle.tland.search_service.entity.enums.PostStatus;
import com.tanle.tland.search_service.entity.enums.PostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

import static com.tanle.tland.search_service.utils.AppConstant.INDEX_NAME;

@Document(indexName = INDEX_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {
    @Id
    private String id;
    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime lastUpdated;

    @Field(type = FieldType.Text)
    private String userId;

    @Field(type = FieldType.Double)
    private double price;

    @Field(type = FieldType.Keyword)
    private PostType type;

    @Field(type = FieldType.Keyword)
    private PostStatus status;

    @Field(type = FieldType.Object)
    private AssetDocument assetDetail;

    @Field(type = FieldType.Object)
    private UserInfoDocument userInfoDocument;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDocument {
        private String id;
        private String firstName;
        private String lastName;
        private String avt;
    }
}
