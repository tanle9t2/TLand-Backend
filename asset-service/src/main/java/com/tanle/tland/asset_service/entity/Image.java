package com.tanle.tland.asset_service.entity;

import jakarta.annotation.Generated;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image implements Content {
    @Id
    private String id;
    private String url;
    private String name;
    @CreatedDate
    private LocalDateTime createdAt;

    @Override
    public void generateId() {
        this.id = UUID.randomUUID().toString();
    }
}
