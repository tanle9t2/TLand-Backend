package com.tanle.tland.post_service.mapper;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.projection.PostHistory;
import com.tanle.tland.post_service.projection.PostOverview;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.PostHistoryResponse;
import com.tanle.tland.post_service.response.PostOverviewResponse;
import com.tanle.tland.post_service.response.PostResponse;
import com.tanle.tland.user_serivce.grpc.PostDetailResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {AssetMapper.class})
public interface PostMapper {

    Post convertToEntity(PostCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePost(PostCreateRequest request, @MappingTarget Post post);

    PostOverviewResponse convertToResponse(PostOverview postOverview);

    PostHistoryResponse convertToResponse(PostHistory postHistory);

    PostResponse convertToPostDetailResponse(Post post);

    PostDetailResponse convertToResponseGrpc(Post post);
}
