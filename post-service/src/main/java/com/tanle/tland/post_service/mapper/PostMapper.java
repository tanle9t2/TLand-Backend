package com.tanle.tland.post_service.mapper;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.projection.PostOverview;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.PostOverviewResponse;
import com.tanle.tland.post_service.response.PostResponse;
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

    PostResponse convertToPostDetailResponse(Post post);
}
