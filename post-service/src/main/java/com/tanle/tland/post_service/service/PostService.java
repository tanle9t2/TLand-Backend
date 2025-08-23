package com.tanle.tland.post_service.service;

import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.*;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

public interface PostService {
    MessageResponse createPost(PostCreateRequest request, String userId, HttpServletRequest httpServletRequest);

    MessageResponse updatePost(String postId, String userId, PostCreateRequest request) throws AccessDeniedException;

    MessageResponse inActivePost(String userId, String postId);

    MessageResponse acceptPost(List<String> role, String postId);

    MessageResponse rejectPost(List<String> role, String postId);

    MessageResponse likePost(String userId, String postId);

    MessageResponse unlikePost(String userId, String postId);

    PostDetailResponse findPostById(String postId);

    PageResponse<PostHistoryResponse> findHistoryPost(String assetId, String userId, int page, int size);

    PageResponse<PostResponse> findAll(int page, int limit, String type);

    List<PostResponse> findAll();

    PageResponse<PostOverviewResponse> findAllByStatus(String status, String kw, String userId, int page, int limit);

    PageResponse<PostAdminOverviewResponse> findAllByStatus(String status, String kw, int page, int limit,
                                                            String orderBy, String orderDirection);

    MessageResponse createComment(String postId, String userId, Map<String, String> content);

    MessageResponse deleteComment(String postId, Map<String, String> content);

    PageResponse<CommentResponse> findCommentsByPostId(String postId, int page, int size);

    List<StatusCountResponse> countStatusPost(String userId);
}
