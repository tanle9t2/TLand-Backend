package com.tanle.tland.post_service.service;

import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.CommentResponse;
import com.tanle.tland.post_service.response.MessageResponse;
import com.tanle.tland.post_service.response.PageResponse;
import com.tanle.tland.post_service.response.PostResponse;

import java.util.Map;

public interface PostService {
    MessageResponse createPost(PostCreateRequest request);

    MessageResponse updatePost(String postId, String userId, Map<String, String> updateRequest);

    MessageResponse inActivePost(String userId, String postId);

    MessageResponse acceptPost(String userId, String postId);

    MessageResponse likePost(String userId, String postId);

    MessageResponse unlikePost(String userId, String postId);

    PostResponse findPostById(String postId);

    PageResponse<PostResponse> findAll(int page, int limit);

    MessageResponse createComment(String postId, Map<String, String> content);

    MessageResponse deleteComment(String postId, Map<String, String> content);

    PageResponse<CommentResponse> findCommentsByPostId(String postId, int page, int size);
}
