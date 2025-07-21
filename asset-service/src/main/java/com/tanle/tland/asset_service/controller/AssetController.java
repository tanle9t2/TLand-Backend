package com.tanle.tland.asset_service.controller;

import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class AssetController {
    private final AssetService assetService;


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
}
