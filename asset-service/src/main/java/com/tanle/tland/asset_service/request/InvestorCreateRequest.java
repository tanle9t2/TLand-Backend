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
public class InvestorCreateRequest {
    private LocalDateTime foundingDate;
    private String name;
    private String description;
}
