package com.tanle.tland.asset_service.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProjectCreateRequest {

    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime openSale;
    private String description;
    private String investorId;
}
