package com.tanle.tland.asset_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "investor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investor {
    @Id
    private String id;
    private LocalDateTime foundingDate;
    private String name;
    private String description;
}
