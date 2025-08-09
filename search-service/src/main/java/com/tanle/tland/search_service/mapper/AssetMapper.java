package com.tanle.tland.search_service.mapper;

import com.tanle.tland.search_service.entity.AssetDocument;
import com.tanle.tland.search_service.mapper.decorator.AssetMapperDecorator;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
@DecoratedWith(AssetMapperDecorator.class)
public interface AssetMapper {
    AssetDocument convertToDocument(AssetResponse response);
}
