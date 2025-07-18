package com.tanle.tland.asset_service.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "contentType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Image.class, name = "image"),
        @JsonSubTypes.Type(value = Video.class, name = "video")
})
public interface Content {
}
