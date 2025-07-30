package com.tanle.tland.asset_service.mapper;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface AssetMapper {

    Asset convertToAsset(AssetCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateConvertAsset(AssetCreateRequest request, @MappingTarget Asset asset);

    AssetDetailResponse convertToDetailResponse(Asset asset);
}
