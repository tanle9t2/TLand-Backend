package com.tanle.tland.search_service.service.impl;


import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.tanle.tland.search_service.entity.PostDocument;
import com.tanle.tland.search_service.entity.enums.PostStatus;
import com.tanle.tland.search_service.mapper.PostMapper;
import com.tanle.tland.search_service.response.FilterSearchResponse;
import com.tanle.tland.search_service.response.PageResponse;
import com.tanle.tland.search_service.service.SearchService;
import com.tanle.tland.search_service.utils.FilterUtils;
import com.tanle.tland.user_serivce.grpc.Empty;
import com.tanle.tland.user_serivce.grpc.PostDetailResponse;
import com.tanle.tland.user_serivce.grpc.PostDetailResponseList;
import com.tanle.tland.user_serivce.grpc.PostToSearchServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.catalina.util.FilterUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import static com.tanle.tland.search_service.utils.AppConstant.INDEX_NAME;
import static com.tanle.tland.search_service.utils.ValidationUtils.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchOperations elasticsearchRestTemplate;
    private final PostMapper postMapper;

    @GrpcClient("postServiceGrpc")
    private PostToSearchServiceGrpc.PostToSearchServiceBlockingStub postToSearchServiceBlockingStub;

    @Override
    public PageResponse<PostDocument> searchPost(String keyword, String page, String size, Map<String, String> params) {
        NativeQueryBuilder queryBuilder = this.extractQuery(keyword, page, size, params);
        SearchHits<PostDocument> searchHits = elasticsearchOperations
                .search(queryBuilder.build(), PostDocument.class);

        var responses = searchHits.getSearchHits()
                .stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());

        long totalElements = elasticsearchOperations.count(queryBuilder.build(), PostDocument.class); //without the affect of pagination

        return PageResponse.<PostDocument>builder()
                .content(responses)
                .page(Integer.parseInt(page))
                .size(Integer.parseInt(size))
                .totalElements(searchHits.getTotalHits())
                .totalPages((int) Math.ceil((double) totalElements / Integer.parseInt(size)))
                .build();
    }

    @Override
    public List<FilterSearchResponse> getAggregation(String keyword) {
        return null;
    }

    @Override
    public void migrateData() {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(INDEX_NAME);
        // Check if the index exists
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(indexCoordinates);
        if (!indexOperations.exists()) {

            indexOperations.create();
            System.out.println("Index created: " + INDEX_NAME);
        }
        PostDetailResponseList responses = postToSearchServiceBlockingStub.getAllPost(Empty.newBuilder().build());

        for (var post : responses.getResponseList()) {
            PostDocument postDocument = postMapper.convertToDocument(post);
            elasticsearchRestTemplate.save(postDocument);
            log.info("Create post - {}", post.getId());
        }

    }

    private boolean isValidSearchField(Map<String, String> params, String keyword) {
        return params != null && !isNullOrEmpty(params.get(keyword));
    }

    private NativeQueryBuilder extractQuery(String keyword, String page, String size, Map<String, String> params) {
        String category = isValidSearchField(params, FilterUtils.CATEGORY) ? params.get(FilterUtils.CATEGORY) : null;
        String province = isValidSearchField(params, FilterUtils.PROVINCE) ? params.get(FilterUtils.PROVINCE) : null;
        String ward = isValidSearchField(params, FilterUtils.WARD) ? params.get(FilterUtils.WARD) : null;
        Double minPrice = isValidSearchField(params, FilterUtils.MIN_PRICE) ? Double.parseDouble(params.get(FilterUtils.MIN_PRICE)) : null;
        Double maxPrice = isValidSearchField(params, FilterUtils.MAX_PRICE) ? Double.parseDouble(params.get(FilterUtils.MAX_PRICE)) : null;

        String sortBy = params != null && !isNullOrEmpty(params.get("sortBy")) ? params.get("sortBy") : null;
        String order = params != null && !isNullOrEmpty(params.get("order")) ? params.get("order") : null;

        var boolQueryBuilder = QueryBuilders.bool();
        boolQueryBuilder
                .must(builder -> builder
                        .term(t -> t
                                .field("status.keyword")
                                .value(PostStatus.SHOW.name())));

        if (keyword != null && !keyword.isEmpty()) {
            var shouldQuery = QueryBuilders.bool();
            boolQueryBuilder
                    .should(QueryBuilders.multiMatch(m -> m
                            .fields("title",
                                    "description",
                                    "assetDocument.province",
                                    "assetDocument.ward",
                                    "assetDocument.address",
                                    "assetDocument.categoryDocument.name",
                                    "sectionDocument.contentDocumentList.name")
                            .query(keyword)
                            .fuzziness("AUTO")
                            .boost(1.2f)))
                    .should(QueryBuilders.matchPhrasePrefix(m -> m
                            .field("title")
                            .query(keyword)
                            .maxExpansions(20)
                            .slop(2)))
                    .minimumShouldMatch("1");
            boolQueryBuilder.must(shouldQuery.build()._toQuery());
        }

        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                .withQuery(boolQueryBuilder.build()._toQuery());

        queryBuilder.withPageable(PageRequest.of(
                Integer.parseInt(page) - 1,
                Integer.parseInt(size)));

        queryBuilder.withFilter(f -> f.bool(b -> {
            extractedTermsFilter(category, "category", b);
            extractedRange(minPrice, maxPrice, "price", b);
            return b;
        }));

        if (sortBy != null) {
            SortOrder sortOrder = order != null && order.equals("desc") ? SortOrder.Desc : SortOrder.Asc;
            queryBuilder.withSort(s -> switch (sortBy) {
                case "newest" -> s.field(f -> f.field("createdAt").order(SortOrder.Desc));
                case "price" -> s.field(f -> f.field("price").order(sortOrder));
                default -> s.score(v -> v.order(SortOrder.Desc));
            });
        }

        return queryBuilder;
    }

    public static void extractedTermsFilter(String fieldValues, String field, BoolQuery.Builder b) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return;
        }
        String[] valuesArray = fieldValues.split(",");
        b.must(m -> {
            BoolQuery.Builder innerBool = new BoolQuery.Builder();
            for (String value : valuesArray) {
                innerBool.should(s -> s
                        .term(t -> t
                                .field(field)
                                .value(value)
                                .caseInsensitive(true)
                        )
                );
            }
            return new Query.Builder().bool(innerBool.build());
        });
    }

    //    0-1,2-3, 4-5,6+
    private void orExtractedRange(String durationRanges, String field, BoolQuery.Builder bool) {
        if (durationRanges == null || durationRanges.isBlank()) return;

        String[] ranges = durationRanges.split(",");

        bool.must(m -> {
            BoolQuery.Builder orBool = new BoolQuery.Builder();

            for (String range : ranges) {
                Pattern pattern = Pattern.compile("^(\\d+)(?:\\+|-(\\d+))?$");
                Matcher matcher = pattern.matcher(range.trim());

                if (matcher.matches()) {
                    Double from = matcher.group(1) != null ? Double.parseDouble(matcher.group(1)) * 3600 : null;
                    Double to = matcher.group(2) != null ? Double.parseDouble(matcher.group(2)) * 3600 : null;

                    orBool.should(s -> s.range(r -> {
                        r.term(t -> t.field(field).from(from != null ? from.toString() : null).to(to != null ? to.toString() : null));
//                        r.field(field).from(from != null ? from.toString() : null).to(to != null ? to.toString() : null);
                        return r;
                    }));
                }
            }

            return new Query.Builder().bool(orBool.build());
        });
    }

    private void extractedRange(Number min, Number max, String field, BoolQuery.Builder bool) {
        if (min != null || max != null) {
            bool.must(m -> m
                    .range(r -> r.term(t -> t.field(field)
                            .from(min != null ? min.toString() : null)
                            .to(max != null ? max.toString() : null))
                    )
            );
        }
    }

}
