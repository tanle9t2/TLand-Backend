package com.tanle.tland.search_service.mapper;

import com.tanle.tland.search_service.entity.PostDocument;
import com.tanle.tland.user_serivce.grpc.PostDetailResponse;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring", uses = {AssetMapper.class})
public interface PostMapper {
    PostDocument convertToDocument(PostDetailResponse postDetailResponse);
}
