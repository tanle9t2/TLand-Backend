package com.tanle.tland.asset_service.controller;

import com.tanle.tland.asset_service.entity.AssetType;
import com.tanle.tland.asset_service.entity.PageResponse;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.request.UploadImageRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.AssetSummaryResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.tanle.tland.asset_service.utils.AppConstant.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class AssetController {
    private final AssetService assetService;

    @GetMapping("/assets")
    public ResponseEntity<PageResponse<AssetSummaryResponse>> getAssets(
            @RequestHeader("X-UserId") String userId,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size
    ) {
        PageResponse<AssetSummaryResponse> response =
                assetService.findAll(userId, Integer.parseInt(page), Integer.parseInt(size));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/assets/draft")
    public ResponseEntity<List<AssetDetailResponse>> getAssetsDraft(@RequestHeader("X-UserId") String userId) {
        String type = String.valueOf(AssetType.DRAFT);
        List<AssetDetailResponse> response = assetService.findAssetByType(type, userId);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/asset/{assetId}")
    public ResponseEntity<AssetDetailResponse> getAssetById(
            @RequestHeader("X-UserId") String userId,
            @PathVariable("assetId") String id) {
        AssetDetailResponse response = assetService.findAssetById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/asset/link-project")
    public ResponseEntity<MessageResponse> linkAssetToProject(@RequestBody Map<String, String> request) {
        String projectId = request.get("projectId");
        String assetId = request.get("assetId");
        MessageResponse response = assetService.linkAssetToProject(assetId, projectId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/asset")
    public ResponseEntity<MessageResponse> createAsset(
            @RequestHeader("X-UserId") String userId,
            @RequestBody AssetCreateRequest request) {
        MessageResponse response = assetService.createAsset(request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/asset")
    public ResponseEntity<MessageResponse> updateAsset(
            @RequestHeader("X-UserId") String userId,
            @RequestBody AssetCreateRequest request) throws AccessDeniedException {
        MessageResponse response = assetService.updateAsset(request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/asset/{assetId}")
    public ResponseEntity<MessageResponse> deleteAsset(
            @RequestHeader("X-UserId") String userId,
            @PathVariable("assetId") String assetId) throws AccessDeniedException {
        MessageResponse response = assetService.deleteAsset(assetId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/asset/upload")
    public ResponseEntity<MessageResponse> UploadAssetMedia(
            @RequestHeader("X-UserId") String userId,
            @RequestParam(value = "assetId", required = false) String id,
            @RequestParam(value = "file") MultipartFile file) {
        MessageResponse response = assetService.uploadMedia(userId, UploadImageRequest.builder()
                .assetId(id)
                .file(file)
                .build());
        return ResponseEntity.ok(response);
    }
}
