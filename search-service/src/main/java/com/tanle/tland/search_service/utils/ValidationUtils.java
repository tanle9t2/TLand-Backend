package com.tanle.tland.search_service.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    public static boolean isNullOrEmpty(String field) {
        return field == null || field.trim().isEmpty();
    }
}
