package com.tanle.tland.post_service.service;

import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.MessageResponse;

public interface PostService {
    MessageResponse createPost(PostCreateRequest request);
}
