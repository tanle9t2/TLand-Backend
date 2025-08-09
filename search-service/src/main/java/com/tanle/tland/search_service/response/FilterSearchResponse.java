package com.tanle.tland.search_service.response;

import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class FilterSearchResponse {
    public String label;
    public String value;
    public List<FilterSearchItem> items;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    public static class FilterSearchItem {
        public String label;
        public String value;
        private Long count;
    }

}
