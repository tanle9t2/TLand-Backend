package com.tanle.tland.post_service.mapper;

import com.tanle.tland.post_service.mapper.decorator.AssetMapperDecorator;
import com.tanle.tland.post_service.response.AssetDetailResponse;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
@DecoratedWith(value = AssetMapperDecorator.class)
public interface AssetMapper {

    AssetDetailResponse convertToResponse(AssetResponse assetResponse);
}
