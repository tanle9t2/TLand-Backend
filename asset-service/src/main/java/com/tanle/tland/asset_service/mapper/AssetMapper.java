package com.tanle.tland.asset_service.mapper;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface AssetMapper {

    Asset convertToAsset(AssetCreateRequest request);

    AssetDetailResponse convertToDetailResponse(Asset asset);
}
