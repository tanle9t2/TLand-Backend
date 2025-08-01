package com.tanle.tland.asset_service.mapper.decorator;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.entity.Category;
import com.tanle.tland.asset_service.entity.Image;
import com.tanle.tland.asset_service.entity.Video;
import com.tanle.tland.asset_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.asset_service.mapper.AssetMapper;
import com.tanle.tland.asset_service.projection.AssetSummary;
import com.tanle.tland.asset_service.repo.CategoryRepo;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.AssetSummaryResponse;
import com.tanle.tland.asset_service.response.CategoryResponse;
import org.springframework.beans.factory.annotation.Autowired;

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
        Category category = categoryRepo.findById(asset.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found category"));

        response.setCategory(CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build());
        return response;
    }
}
