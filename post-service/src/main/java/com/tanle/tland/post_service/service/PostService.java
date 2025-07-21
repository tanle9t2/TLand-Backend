package com.tanle.tland.post_service.service;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.MessageResponse;
import com.tanle.tland.post_service.response.PostResponse;

import java.util.Map;

public interface PostService {
    MessageResponse createPost(PostCreateRequest request);

    MessageResponse updatePost(String postId, String userId, Map<String, String> updateRequest);

    MessageResponse inActivePost(String userId, String postId);

    MessageResponse acceptPost(String userId, String postId);

    PostResponse findPostById(String postId);
}
