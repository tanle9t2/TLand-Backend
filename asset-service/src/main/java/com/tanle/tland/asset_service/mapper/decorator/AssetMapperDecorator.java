package com.tanle.tland.asset_service.mapper.decorator;

import com.tanle.tland.asset_service.entity.*;
import com.tanle.tland.asset_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.asset_service.mapper.AssetMapper;
import com.tanle.tland.asset_service.projection.AssetSummary;
import com.tanle.tland.asset_service.repo.CategoryRepo;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.AssetSummaryResponse;
import com.tanle.tland.asset_service.response.CategoryResponse;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import com.tanle.tland.user_serivce.grpc.Content;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AssetMapperDecorator implements AssetMapper {
    @Autowired
    private AssetMapper assetMapper;
    @Autowired
    private CategoryRepo categoryRepo;


    @Override
    public AssetSummaryResponse convertToAssetSummaryResponse(AssetSummary assetSummary) {
        AssetSummaryResponse response = assetMapper.convertToAssetSummaryResponse(assetSummary);
        String url = assetSummary.getContents().stream()
                .filter(c -> c instanceof Image)
                .map(c -> (Image) c)
                .findFirst().get().getUrl();
        response.setImageUrl(url);
        response.setTotalImages(assetSummary.getContents().size());

        return response;
    }

    @Override
    public AssetDetailResponse convertToDetailResponse(Asset asset) {
        AssetDetailResponse response = assetMapper.convertToDetailResponse(asset);
        if (asset.getCategoryId() != null) {
            Category category = categoryRepo.findById(asset.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundExeption("Not found category"));


            response.setCategory(CategoryResponse.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .build());
        }
        return response;
    }

    @Override
    public AssetResponse convertToResponseGrpc(Asset asset) {
        AssetResponse.Builder response = assetMapper.convertToResponseGrpc(asset).toBuilder();
        response.addAllDimension(Arrays.stream(asset.getDimension())
                .boxed()
                .collect(Collectors.toList()));
        if (asset.getProperties() != null) {
            response.putAllProperties(asset.getProperties());
        }
        if (asset.getLocationAsset() != null) {
            response.putAllLocationAsset(asset.getLocationAsset());
        }

        if (asset.getCategoryId() != null) {
            com.tanle.tland.user_serivce.grpc.CategoryResponse categoryResponse = categoryRepo.findById(asset.getCategoryId())
                    .map(c -> com.tanle.tland.user_serivce.grpc.CategoryResponse.newBuilder()
                            .setId(c.getId())
                            .setName(c.getName())
                            .build())
                    .get();

            response.setCategory(categoryResponse);
        }
        if (asset.getOtherInfo() != null) {
            response.addAllOtherInfo(Arrays.asList(asset.getOtherInfo()));
        }

        List<Content> contentList = asset.getContents().stream()
                .map(c -> {
                    Content.Builder builder = Content.newBuilder();

                    if (c instanceof Video video) {
                        builder.setId(video.getId())
                                .setDuration(video.getDuration())
                                .setName(video.getName())
                                .setType(ContentType.VIDEO.name())
                                .setUrl(video.getUrl());
                    } else if (c instanceof Image image) {
                        builder.setId(image.getId())
                                .setDuration(0) // or Duration.getDefaultInstance() if it's a message
                                .setName(image.getName())
                                .setType(ContentType.IMAGE.name())
                                .setUrl(image.getUrl());
                    } else {
                        throw new IllegalArgumentException("Unknown content type: " + c.getClass());
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
        response.addAllContentList(contentList);

        return response.build();
    }
}
