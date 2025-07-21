package com.tanle.tland.asset_service.service.impl;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.asset_service.mapper.AssetMapper;
import com.tanle.tland.asset_service.repo.AssetRepo;
import com.tanle.tland.asset_service.repo.ProjectRepo;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.AssetService;
import com.tanle.tland.asset_service.service.ProjectService;
import com.tanle.tland.user_serivce.grpc.UserRequest;
import com.tanle.tland.user_serivce.grpc.UserResponse;
import com.tanle.tland.user_serivce.grpc.UserToAssetServiceGrpc;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {
    @GrpcClient("userServiceGrpc")
    private UserToAssetServiceGrpc.UserToAssetServiceBlockingStub serviceBlockingStub;
    private final AssetRepo assetRepo;
    private final AssetMapper assetMapper;
    private final ProjectRepo projectRepo;

    @Override
    public AssetDetailResponse findAssetById(String id) {
        Asset asset = assetRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found asset"));

        return assetMapper.convertToDetailResponse(asset);
    }

    @Override
    public MessageResponse linkAssetToProject(String assetId, String projectId) {
        projectRepo.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found project: " + projectId));

        Asset asset = assetRepo.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found asset"));

        asset.setProjectId(projectId);
        assetRepo.save(asset);
        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully link asset to project")
                .build();
    }

    @Override
    public MessageResponse createAsset(AssetCreateRequest createRequest) {
        UserResponse userResponse = serviceBlockingStub.getUserById(UserRequest.newBuilder()
                .setId(createRequest.getUserId())
                .build());

        Asset asset = assetMapper.convertToAsset(createRequest);
        asset.setId(UUID.randomUUID().toString());
        assetRepo.save(asset);
        return MessageResponse.builder()
                .status(HttpStatus.CREATED)
                .message("Successfully create asset")
                .build();
    }
}
