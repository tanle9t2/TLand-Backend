package com.tanle.tland.search_service.service.impl;


import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import com.tanle.tland.search_service.entity.PostDocument;
import com.tanle.tland.search_service.entity.enums.PostStatus;
import com.tanle.tland.search_service.entity.enums.PostType;
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
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import static com.tanle.tland.search_service.utils.AppConstant.INDEX_NAME;
import static com.tanle.tland.search_service.utils.FilterUtils.*;
import static com.tanle.tland.search_service.utils.ValidationUtils.*;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchOperations elasticsearchRestTemplate;


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

    private void addOptionalAggregation(
            NativeQueryBuilder builder,
            String aggName,
            String paramValue,
            Function<String, Aggregation> aggregationSupplier
    ) {
        if (paramValue != null && !paramValue.isEmpty()) {
            builder.withAggregation(aggName, aggregationSupplier.apply(paramValue));
        }
    }

    @Override
    public List<FilterSearchResponse> getAggregation(String keyword, Map<String, String> params) {
        NativeQueryBuilder queryBuilder = this.extractQuery(keyword, PAGE, "1", params);
        addOptionalAggregation(
                queryBuilder,
                WARD,
                params.get(PROVINCE),
                province -> Aggregation.of(a -> a
                        .filter(fa -> fa
                                .term(t -> t
                                        .field("assetDetail.province")
                                        .value(province)
                                )
                        )
                        .aggregations("ward", agg -> agg
                                .terms(t -> t
                                        .field("assetDetail.ward")
                                        .size(20)
                                )
                        )
                )
        );
        queryBuilder.withAggregation(PROPERTIES, Aggregation.of(a -> a
                .terms(t -> t
                        .script(s -> s
                                .source("""
                                            if (params._source?.assetDetail?.properties != null) {
                                                return new ArrayList(params._source.assetDetail.properties.keySet());
                                            }
                                            return [];
                                        """)
                        )
                        .size(50)
                )
        ));

        NativeQuery query = queryBuilder.build();

        SearchHits<PostDocument> searchHits = elasticsearchOperations.search(query, PostDocument.class);
        List<org.springframework.data.elasticsearch.client.elc.Aggregation> aggregations = new ArrayList<>();

        if (searchHits.hasAggregations()) {
            ((List<ElasticsearchAggregation>) searchHits.getAggregations().aggregations())
                    .forEach(elsAgg -> aggregations.add(elsAgg.aggregation()));
        }


        List<FilterSearchResponse> responses = new ArrayList<>();

        for (org.springframework.data.elasticsearch.client.elc.Aggregation agg : aggregations) {
            String aggName = agg.getName();
            Aggregate innerAgg = agg.getAggregate();
            List<FilterSearchResponse.FilterSearchItem> filterResponseItems = new ArrayList<>();

            switch (innerAgg._kind()) {
                case Sterms:
                    StringTermsAggregate stringTermsAgg = (StringTermsAggregate) innerAgg._get();
                    List<StringTermsBucket> stringBuckets = (List<StringTermsBucket>) stringTermsAgg.buckets()._get();
                    if (aggName.equals(PROPERTIES))
                        extractAggregationWithFetch(filterResponseItems, stringBuckets, keyword, params);
                    else
                        extractAggregation(aggName, filterResponseItems, stringBuckets);
                    break;
                case Filter:
                    FilterAggregate filterAggregate = (FilterAggregate) innerAgg._get();
                    Map<String, Aggregate> mpAgg = filterAggregate.aggregations();
                    for (var x : mpAgg.entrySet()) {
                        StringTermsAggregate filterTermAggregate = (StringTermsAggregate) x.getValue()._get();
                        List<StringTermsBucket> filterBucket = (List<StringTermsBucket>) filterTermAggregate.buckets()._get();

                        FilterSearchResponse.FilterSearchItem item = FilterSearchResponse.FilterSearchItem
                                .builder()
                                .label(aggName)
                                .build();
                        filterResponseItems.add(item);
                        extractSubValue(item, filterBucket);
                    }
                    break;
                case Range:
                    List<RangeBucket> rangeBuckets = (List<RangeBucket>) innerAgg.range().buckets()._get();
                    for (RangeBucket bucket : rangeBuckets) {
                        filterResponseItems.add(FilterSearchResponse.FilterSearchItem
                                .builder()
                                .label(bucket.key())
                                .value(bucket.key())
                                .count(bucket.docCount())
                                .build());
                    }
                    break;
                default:
                    break;
            }
            responses.add(FilterSearchResponse
                    .builder()
                    .label(aggName)
                    .value(AGGS_VALUE.get(aggName))
                    .items(filterResponseItems)
                    .build());
        }

        return responses;
    }

    private void extractAggregation(String key, List<FilterSearchResponse.FilterSearchItem> filterResponseItems, List<StringTermsBucket> stringBuckets) {
        for (StringTermsBucket bucket : stringBuckets) {
            FilterSearchResponse.FilterSearchItem item = FilterSearchResponse.FilterSearchItem
                    .builder()
                    .label(key)
                    .value(bucket.key().stringValue())
                    .count(bucket.docCount())
                    .build();
            if (!bucket.aggregations().isEmpty()) {
                StringTermsAggregate nameAgg = (StringTermsAggregate) bucket.aggregations().get("name")._get();
                item.setLabel(nameAgg.buckets().array().get(0).key().stringValue());
            }
            filterResponseItems.add(item);
        }
    }

    private void extractSubValue(FilterSearchResponse.FilterSearchItem filterResponseItems,
                                 List<StringTermsBucket> stringBuckets) {
        Set<String> value = new HashSet<>();
        for (StringTermsBucket bucket : stringBuckets) {
            value.add(bucket.key().stringValue());
        }
        filterResponseItems.setValue(value);
    }

    private void extractAggregationWithFetch(List<FilterSearchResponse.FilterSearchItem> filterResponseItems,
                                             List<StringTermsBucket> stringBuckets, String keyword, Map<String, String> params) {
        for (StringTermsBucket bucket : stringBuckets) {
            String propertyKey = bucket.key().stringValue();
            NativeQuery query = this.extractQuery(keyword, PAGE, "1", params)
                    .withAggregation("values_for_key", Aggregation.of(a -> a
                            .terms(t -> t
                                    .script(s -> s
                                            .source(String.format("""
                                                        def props = params._source.assetDetail?.properties;
                                                        if (props != null && props.containsKey('%s')) {
                                                            return props['%s'];
                                                        }
                                                        return null;
                                                    """, propertyKey, propertyKey))
                                    )
                                    .size(50)
                            )
                    ))
                    .build();
            FilterSearchResponse.FilterSearchItem item = FilterSearchResponse.FilterSearchItem
                    .builder()
                    .label(propertyKey)
                    .build();
            filterResponseItems.add(item);

            SearchHits<PostDocument> searchHits = elasticsearchOperations.search(query, PostDocument.class);

            List<org.springframework.data.elasticsearch.client.elc.Aggregation> aggregations = new ArrayList<>();
            if (searchHits.hasAggregations()) {
                ((List<ElasticsearchAggregation>) searchHits.getAggregations().aggregations())
                        .forEach(elsAgg -> aggregations.add(elsAgg.aggregation()));
            }

            for (var x : aggregations) {
                StringTermsAggregate stringTermsAgg = (StringTermsAggregate) x.getAggregate()._get();
                List<StringTermsBucket> subBuckets = (List<StringTermsBucket>) stringTermsAgg.buckets()._get();
                extractSubValue(item, subBuckets);
            }
        }
    }


    private boolean isValidSearchField(Map<String, String> params, String keyword) {
        return params != null && !isNullOrEmpty(params.get(keyword));
    }

    private NativeQueryBuilder extractQuery(String keyword) {
        return this.extractQuery(keyword, FilterUtils.PAGE, FilterUtils.PAGE_SIZE, null);
    }

    private NativeQueryBuilder extractQuery(String keyword, String page, String size, Map<String, String> params) {
        String category = isValidSearchField(params, FilterUtils.CATEGORY) ? params.get(FilterUtils.CATEGORY) : null;
        String province = isValidSearchField(params, FilterUtils.PROVINCE) ? params.get(FilterUtils.PROVINCE) : null;
        String type = isValidSearchField(params, TYPE) ? params.get(TYPE) : PostType.SELL.name();
        String ward = isValidSearchField(params, FilterUtils.WARD) ? params.get(FilterUtils.WARD) : null;
        Double minPrice = isValidSearchField(params, FilterUtils.MIN_PRICE) ? Double.parseDouble(params.get(FilterUtils.MIN_PRICE)) : null;
        Double maxPrice = isValidSearchField(params, FilterUtils.MAX_PRICE) ? Double.parseDouble(params.get(FilterUtils.MAX_PRICE)) : null;

        String sortBy = isValidSearchField(params, ORDER_BY) ? params.get(ORDER_BY) : null;
        String order = isValidSearchField(params, ORDER) ? params.get(ORDER) : null;

        //get rest param for filtering properties
        Map<String, String> restParams = params.entrySet().stream()
                .filter(e -> !KNOW_KEY.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var boolQueryBuilder = QueryBuilders.bool();
        if (keyword != null && !keyword.isEmpty()) {
            var shouldQuery = QueryBuilders.bool();
            boolQueryBuilder
                    .should(QueryBuilders.multiMatch(m -> m
                            .fields("title",
                                    "description",
                                    "assetDetail.province",
                                    "assetDetail.ward",
                                    "assetDetail.address",
                                    "assetDetail.categoryDocument.name")
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
        if (category != null) {
            boolQueryBuilder
                    .must(builder -> builder
                            .term(t -> t
                                    .field("assetDetail.category.id.keyword")
                                    .value(category)));
        }

        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                .withQuery(boolQueryBuilder.build()._toQuery());

        queryBuilder.withPageable(PageRequest.of(
                Integer.parseInt(page),
                Integer.parseInt(size)));

        queryBuilder.withFilter(f -> f.bool(b -> {
            extractedTermsFilter(PostStatus.SHOW.name(), "status", b);
            extractedTermsFilter(province, "assetDetail.province", b);
            extractedTermsFilter(ward, "assetDetail.ward", b);
            extractedTermsFilter(type, "type", b);
            extractedRange(minPrice, maxPrice, "price", b);

            for (var x : restParams.entrySet()) {
                extractedTermsFilter(x.getValue(), String.format("assetDetail.properties.%s", x.getKey()), b);
            }
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
            bool.must(m -> m.range(r -> {
                r.term(t -> {
                    t.field(field);
                    if (min != null) {
                        t.gte(JsonData.of(min).toString());
                    }
                    if (max != null) {
                        t.lte(JsonData.of(max).toString());
                    }
                    return t;
                });
                return r;
            }));
        }
    }

}
