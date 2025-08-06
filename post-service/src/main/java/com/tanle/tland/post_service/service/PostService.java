package com.tanle.tland.post_service.service;

import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

public interface PostService {
    MessageResponse createPost(PostCreateRequest request);

    MessageResponse updatePost(String postId, String userId, PostCreateRequest request) throws AccessDeniedException;

    MessageResponse inActivePost(String userId, String postId);

    MessageResponse acceptPost(String userId, String postId);

    MessageResponse likePost(String userId, String postId);

    MessageResponse unlikePost(String userId, String postId);

    PostResponse findPostById(String postId);

    PageResponse<PostResponse> findAll(int page, int limit, String type);

    PageResponse<PostOverviewResponse> findAllByStatus(String status, String kw, String userId, int page, int limit);

    MessageResponse createComment(String postId, Map<String, String> content);

    MessageResponse deleteComment(String postId, Map<String, String> content);

    PageResponse<CommentResponse> findCommentsByPostId(String postId, int page, int size);

    List<StatusCountResponse> countStatusPost(String userId);
}
