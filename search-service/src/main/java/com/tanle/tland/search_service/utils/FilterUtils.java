package com.tanle.tland.search_service.utils;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FilterUtils {
    public static final String DEFAULT_BANNER = "https://res.cloudinary.com/dlwjpzshh/image/upload/v1746793532/jjvsildtdhpto8nr13pm.png";
    public static final String PAGE = "1";
    public static final String PAGE_SIZE = "5";
    public static final String PADDING_OFFSET = "0";
    public static final String CATEGORY = "category";
    public static final String PROPERTIES = "properties";
    public static final String MIN_PRICE = "minPrice";
    public static final String MAX_PRICE = "maxPrice";
    public static final String PROVINCE = "province";
    public static final String WARD = "ward";
    public static final String TYPE = "type";
    public static final String ORDER_BY = "sortBy";
    public static final String ORDER = "order";

    public static final Set<String> KNOW_KEY = Set.of(
            CATEGORY,
            PROVINCE,
            TYPE,
            WARD,
            MIN_PRICE,
            MAX_PRICE,
            ORDER_BY,
            ORDER,
            "page",
            "size"

    );

    public static final Map<String, String> AGGS_VALUE = new HashMap<>() {{
        put("ward", "Phường");
    }};
}
