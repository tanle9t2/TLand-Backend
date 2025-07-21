package com.tanle.tland.asset_service.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProjectResponse {
    private String id;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime openSale;
    private String description;
    private List<AssetDetailResponse> assetList;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class InvestorDetail {
        private String id;
        private LocalDateTime foundingDate;
        private String name;
        private String description;
    }
}
