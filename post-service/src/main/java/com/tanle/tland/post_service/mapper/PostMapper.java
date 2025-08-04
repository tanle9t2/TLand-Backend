package com.tanle.tland.post_service.mapper;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.projection.PostOverview;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.PostOverviewResponse;
import com.tanle.tland.post_service.response.PostResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {AssetMapper.class})
public interface PostMapper {

    Post convertToEntity(PostCreateRequest request);

    PostOverviewResponse convertToResponse(PostOverview postOverview);

    PostResponse convertToPostDetailResponse(Post post);
}
