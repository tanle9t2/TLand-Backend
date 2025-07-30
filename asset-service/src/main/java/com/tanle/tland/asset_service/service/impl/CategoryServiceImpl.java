package com.tanle.tland.asset_service.service.impl;

import com.tanle.tland.asset_service.entity.Category;
import com.tanle.tland.asset_service.repo.CategoryRepo;
import com.tanle.tland.asset_service.response.CategoryResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepo categoryRepo;

    @Override
    public MessageResponse createCategory(Map<String, String> category) throws BadRequestException {
        if (!category.containsKey("name"))
            throw new BadRequestException("Missing name category");

        Category entity = Category.builder()
                .id(UUID.randomUUID().toString())
                .name(category.get("name"))
                .build();
        categoryRepo.save(entity);
        return MessageResponse.builder()
                .data(entity)
                .message("Successfully create category")
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    public List<CategoryResponse> findAll() {
        List<CategoryResponse> categoryResponses = categoryRepo.findAll().stream()
                .map(c -> CategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .build()).collect(Collectors.toList());

        return categoryResponses;
    }
}
