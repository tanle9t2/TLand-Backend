package com.tanle.tland.search_service.controller;

import com.tanle.tland.search_service.entity.PostDocument;
import com.tanle.tland.search_service.response.FilterSearchResponse;
import com.tanle.tland.search_service.response.PageResponse;
import com.tanle.tland.search_service.service.SearchService;
import com.tanle.tland.search_service.utils.FilterUtils;
import com.tanle.tland.user_serivce.grpc.PostDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = FilterUtils.PAGE) String page,
            @RequestParam(value = "size", required = false, defaultValue = FilterUtils.PAGE_SIZE) String size,
            @RequestParam Map<String, String> params
    ) {
        PageResponse<PostDocument> post = searchService.searchPost(keyword, page, size, params);


        return ResponseEntity.ok(post);
    }

    @GetMapping("/search/filters")
    public ResponseEntity<?> getFilters(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = FilterUtils.PAGE) String page,
            @RequestParam(value = "size", required = false, defaultValue = FilterUtils.PAGE_SIZE) String size,
            @RequestParam Map<String, String> params
    ) {
        List<FilterSearchResponse> filterSearchResponses = searchService.getAggregation(keyword,params);


        return ResponseEntity.ok(filterSearchResponses);
    }

    @GetMapping("/search/async")
    public ResponseEntity<String> asyncData() {
        searchService.migrateData();
        return ResponseEntity.ok("OK");
    }
}
