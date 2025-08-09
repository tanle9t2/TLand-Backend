package com.tanle.tland.search_service.utils;


import java.util.HashMap;
import java.util.Map;

public class FilterUtils {
    public static final String DEFAULT_BANNER = "https://res.cloudinary.com/dlwjpzshh/image/upload/v1746793532/jjvsildtdhpto8nr13pm.png";
    public static final String PAGE = "1";
    public static final String PAGE_SIZE = "5";
    public static final String PADDING_OFFSET = "0";
    public static final String CATEGORY = "categories";
    public static final String PROPERTIES = "properties";
    public static final String MIN_PRICE = "minPrice";
    public static final String MAX_PRICE = "maxPrice";
    public static final String PROVINCE = "province";
    public static final String WARD = "ward";
    public static final Map<String, String> AGGS_VALUE = new HashMap<>() {{
        put("Rating", "rating");
        put("Duration(Hours)", "duration");
        put("Level", "level");
        put("Category", "category");
        put("Teacher", "teacher");
    }};
}
