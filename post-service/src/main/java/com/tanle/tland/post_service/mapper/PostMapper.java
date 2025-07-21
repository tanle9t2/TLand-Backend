package com.tanle.tland.post_service.mapper;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.request.PostCreateRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {

    Post convertToEntity(PostCreateRequest request);
}
