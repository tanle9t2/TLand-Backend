package com.tanle.tland.asset_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video implements Content {
    @Id
    private String id;
    private String url;
    private String name;
    @CreatedDate
    private LocalDateTime createdAt;
    private double duration;
    @Override
    public void generateId() {
        this.id = UUID.randomUUID().toString();
    }
}
