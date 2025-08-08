package com.tanle.tland.asset_service.service;

import com.tanle.tland.asset_service.entity.AssetType;
import com.tanle.tland.asset_service.entity.PageResponse;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.request.UploadImageRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.AssetSummaryResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import org.apache.logging.log4j.message.Message;
import org.springframework.data.domain.Page;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface AssetService {
    AssetDetailResponse findAssetById(String id);

    List<AssetDetailResponse> findAssetByType(String type, String userId);

    PageResponse<AssetSummaryResponse> findAll(String userId, int page, int size);

    MessageResponse deleteAsset(String assetId, String userId) throws AccessDeniedException;

    MessageResponse linkAssetToProject(String assetId, String projectId);

    MessageResponse createAsset(AssetCreateRequest createRequest,String userId);

    MessageResponse uploadMedia(String userId, UploadImageRequest request);

    MessageResponse updateAsset(AssetCreateRequest request, String userId) throws AccessDeniedException;
}
