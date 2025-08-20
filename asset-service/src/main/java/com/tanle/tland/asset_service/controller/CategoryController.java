package com.tanle.tland.asset_service.controller;

import com.tanle.tland.asset_service.response.CategoryResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    @PostMapping("/category")
    public ResponseEntity<MessageResponse> createCategory(@RequestBody Map<String, String> category) throws BadRequestException {
        MessageResponse response = categoryService.createCategory(category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategory(String username) {
        List<CategoryResponse> response = categoryService.findAll();
        return ResponseEntity.ok(response);
    }
}
