package com.tanle.tland.asset_service.service.impl;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.asset_service.mapper.AssetMapper;
import com.tanle.tland.asset_service.repo.AssetRepo;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {
    private final AssetRepo assetRepo;
    private final AssetMapper assetMapper;

    @Override
    public AssetDetailResponse findAssetById(String id) {
        Asset asset = assetRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found asset"));

        return assetMapper.convertToDetailResponse(asset);
    }

    @Override
    public MessageResponse createAsset(AssetCreateRequest createRequest) {
        Asset asset = assetMapper.convertToAsset(createRequest);
        asset.setId(UUID.randomUUID().toString());
        assetRepo.save(asset);
        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully create asset")
                .build();
    }
}
