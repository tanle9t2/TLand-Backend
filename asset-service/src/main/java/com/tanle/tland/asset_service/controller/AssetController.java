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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class AssetController {
    private final AssetService assetService;


    @GetMapping("/assets")
    public ResponseEntity<PageResponse<AssetSummaryResponse>> getAssets(
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size
    ) {
        String userId = "eadd6456-a5ea-4d41-b71a-061541227b8d";
        PageResponse<AssetSummaryResponse> response =
                assetService.findAll(userId, Integer.parseInt(page), Integer.parseInt(size));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/assets/draft")
    public ResponseEntity<List<AssetDetailResponse>> getAssetsDraft() {
        String userId = "eadd6456-a5ea-4d41-b71a-061541227b8d";
        String type = String.valueOf(AssetType.DRAFT);
        List<AssetDetailResponse> response = assetService.findAssetByType(type, userId);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/asset/{assetId}")
    public ResponseEntity<AssetDetailResponse> getAssetById(@PathVariable("assetId") String id) {
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
    public ResponseEntity<MessageResponse> createAsset(@RequestBody AssetCreateRequest request) {
        MessageResponse response = assetService.createAsset(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/asset")
    public ResponseEntity<MessageResponse> updateAsset(@RequestBody AssetCreateRequest request) {
        MessageResponse response = assetService.updateAsset(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/asset/upload")
    public ResponseEntity<MessageResponse> uploadAssetImage(
            @RequestParam(value = "assetId", required = false) String id,
            @RequestParam(value = "file") MultipartFile file) {
        MessageResponse response = assetService.uploadImage(UploadImageRequest.builder()
                .assetId(id)
                .file(file)
                .build());
        return ResponseEntity.ok(response);
    }
}
