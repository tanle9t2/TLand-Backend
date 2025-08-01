package com.tanle.tland.asset_service.service.impl;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.entity.Image;
import com.tanle.tland.asset_service.entity.Video;
import com.tanle.tland.asset_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.asset_service.repo.AssetRepo;
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

        List<Content> contentList = asset.getContents().stream()
                .map(c -> {
                    Content.Builder builder = Content.newBuilder();

                    if (c instanceof Video video) {
                        builder.setId(video.getId())
                                .setDuration(video.getDuration())
                                .setName(video.getName())
                                .setType("Video")
                                .setUrl(video.getUrl());
                    } else if (c instanceof Image image) {
                        builder.setId(image.getId())
                                .setDuration(0) // or Duration.getDefaultInstance() if it's a message
                                .setName(image.getName())
                                .setType("Image")
                                .setUrl(image.getUrl());
                    } else {
                        throw new IllegalArgumentException("Unknown content type: " + c.getClass());
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());


        AssetResponse response = AssetResponse.newBuilder()
                .addAllContentList(contentList)
                .setAddress(asset.getAddress())
                .setName(asset.getName())
                .setDescription(asset.getDescription())
                .setProvince(asset.getProvince())
                .setWard(asset.getWard())
                .addAllDimension(Arrays.stream(asset.getDimension())
                        .boxed()
                        .collect(Collectors.toList()))
                .putAllProperties(asset.getProperties())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
