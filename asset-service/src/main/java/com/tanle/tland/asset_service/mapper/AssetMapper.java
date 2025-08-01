package com.tanle.tland.asset_service.mapper;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.mapper.decorator.AssetMapperDecorator;
import com.tanle.tland.asset_service.projection.AssetSummary;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.AssetSummaryResponse;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@DecoratedWith(AssetMapperDecorator.class)
public interface AssetMapper {

    Asset convertToAsset(AssetCreateRequest request);

    AssetSummaryResponse convertToAssetSummaryResponse(AssetSummary assetSummary);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateConvertAsset(AssetCreateRequest request, @MappingTarget Asset asset);

    AssetDetailResponse convertToDetailResponse(Asset asset);
}
