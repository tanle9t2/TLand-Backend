package com.tanle.tland.asset_service.service;

import com.tanle.tland.asset_service.response.CategoryResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import org.apache.coyote.BadRequestException;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    MessageResponse createCategory(Map<String, String> category) throws BadRequestException;

    List<CategoryResponse> findAll();
}
