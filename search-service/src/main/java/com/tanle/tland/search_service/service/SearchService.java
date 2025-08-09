package com.tanle.tland.search_service.service;

import com.tanle.tland.search_service.entity.PostDocument;
import com.tanle.tland.search_service.response.FilterSearchResponse;
import com.tanle.tland.search_service.response.PageResponse;

import java.util.List;
import java.util.Map;

public interface SearchService {
    PageResponse<PostDocument> searchPost(String keyword, String page, String size, Map<String, String> params);

    List<FilterSearchResponse> getAggregation(String keyword);

    void migrateData();
}
