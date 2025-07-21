package com.tanle.tland.asset_service.service;

import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import org.apache.logging.log4j.message.Message;

public interface AssetService {
    AssetDetailResponse findAssetById(String id);

    MessageResponse linkAssetToProject(String assetId, String projectId);

    MessageResponse createAsset(AssetCreateRequest createRequest);
}
