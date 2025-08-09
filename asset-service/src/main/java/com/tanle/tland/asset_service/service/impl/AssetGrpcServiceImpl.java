package com.tanle.tland.asset_service.service.impl;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.entity.ContentType;
import com.tanle.tland.asset_service.entity.Image;
import com.tanle.tland.asset_service.entity.Video;
import com.tanle.tland.asset_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.asset_service.mapper.AssetMapper;
import com.tanle.tland.asset_service.repo.AssetRepo;
import com.tanle.tland.asset_service.repo.CategoryRepo;
import com.tanle.tland.user_serivce.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class AssetGrpcServiceImpl extends AssetToPostServiceGrpc.AssetToPostServiceImplBase {
    private final AssetRepo assetRepo;
    private final CategoryRepo categoryRepo;
    private final AssetMapper assetMapper;

    @Override
    public void getPoster(AssetRequest request, StreamObserver<Content> responseObserver) {
        Optional<Asset> optionalAsset = assetRepo.findById(request.getId());
        if (!optionalAsset.isPresent())
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Asset not found with ID: " + request.getId())
                            .asRuntimeException());

        if (!optionalAsset.get().getUserId().equals(request.getUserId()))
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription("Don't have permission for this resource")
                            .asRuntimeException());

        Asset asset = optionalAsset.get();
        if (asset.getContents() == null)
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Don't have image " + request.getId())
                            .asRuntimeException());
        Image image = asset.getPoster();
        Content content = Content.newBuilder()
                .setType(ContentType.IMAGE.name())
                .setUrl(image.getUrl())
                .setId(image.getId())
                .build();

        responseObserver.onNext(content);
        responseObserver.onCompleted();
    }

    @Override
    public void checkExisted(AssetRequest request, StreamObserver<CheckExistedResponse> responseObserver) {
        Optional<Asset> optionalAsset = assetRepo.findById(request.getId());
        if (!optionalAsset.isPresent())
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Asset not found with ID: " + request.getId())
                            .asRuntimeException());

        if (!optionalAsset.get().getUserId().equals(request.getUserId()))
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription("Don't have permission for this resource")
                            .asRuntimeException());
        responseObserver.onNext(CheckExistedResponse.newBuilder()
                .setIsExisted(true)
                .setId(request.getId())
                .build());

        responseObserver.onCompleted();
    }

    @Override
    public void getAssetDetail(AssetRequest request, StreamObserver<AssetResponse> responseObserver) {
        Optional<Asset> optionalAsset = assetRepo.findById(request.getId());
        if (!optionalAsset.isPresent())
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Asset not found with ID: " + request.getId())
                            .asRuntimeException());

        Asset asset = optionalAsset.get();
        AssetResponse response = assetMapper.convertToResponseGrpc(asset);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
