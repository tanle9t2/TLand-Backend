package com.tanle.tland.post_service.service.impl;


import com.tanle.tland.post_service.entity.*;
import com.tanle.tland.post_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.post_service.exception.UnauthorizedException;
import com.tanle.tland.post_service.mapper.AssetMapper;
import com.tanle.tland.post_service.mapper.PostMapper;
import com.tanle.tland.post_service.projection.PostOverview;
import com.tanle.tland.post_service.projection.StatusCount;
import com.tanle.tland.post_service.repo.CommentRepo;
import com.tanle.tland.post_service.repo.PostRepo;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.*;
import com.tanle.tland.post_service.service.PostService;
import com.tanle.tland.user_serivce.grpc.AssetRequest;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import com.tanle.tland.user_serivce.grpc.AssetToPostServiceGrpc;
import com.tanle.tland.user_serivce.grpc.Content;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    @GrpcClient("assetService")
    private AssetToPostServiceGrpc.AssetToPostServiceBlockingStub assetToPostServiceBlockingStub;
    private final PostRepo postRepo;
    private final CommentRepo commentRepo;
    private final PostMapper postMapper;
    private final AssetMapper assetMapper;


    @Override
    @Transactional
    public MessageResponse createPost(PostCreateRequest request) {
        assetToPostServiceBlockingStub.checkExisted(AssetRequest.newBuilder()
                .setId(request.getAssetId())
                .setUserId(request.getUserId())
                .build());

        Post post = postMapper.convertToEntity(request);
        post.setStatus(PostStatus.WAITING_PAYMENT);
        postRepo.save(post);

        return MessageResponse.builder()
                .message("Successfully create post")
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse updatePost(String postId, String userId, Map<String, String> updateRequest) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));
        if (!post.getUserId().equals(userId))
            throw new UnauthorizedException("Don't have permission for this resource");

        if (updateRequest.containsKey("title")) {
            post.setTitle(updateRequest.get("title"));
        }

        if (updateRequest.containsKey("description")) {
            post.setTitle(updateRequest.get("description"));
        }
        postRepo.save(post);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .data(updateRequest)
                .message("Successfully update post")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse inActivePost(String userId, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        if (!post.getUserId().equals(userId))
            throw new UnauthorizedException("Don't have permission for this resource");

        post.setStatus(PostStatus.DELETE);
        return MessageResponse.builder()
                .message("Successfully inactive post")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public MessageResponse acceptPost(String userId, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        if (!post.getUserId().equals(userId))
            throw new UnauthorizedException("Don't have permission for this resource");

        post.setStatus(PostStatus.SHOW);
        return MessageResponse.builder()
                .message("Successfully accept post")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse likePost(String userId, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        PostLike postLike = PostLike.builder()
                .postId(postId)
                .userId(userId)
                .build();

        post.addLikePost(postLike);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully like post")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse unlikePost(String userId, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        post.removeLikePost(userId);
        postRepo.save(post);
        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully like post")
                .build();
    }

    @Override
    public PostResponse findPostById(String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        AssetResponse assetResponse = assetToPostServiceBlockingStub.getAssetDetail(AssetRequest.newBuilder()
                .setId(post.getAssetId())
                .build());

        PostResponse postResponse = postMapper.convertToPostDetailResponse(post);
        postResponse.setAssetDetail(assetMapper.convertToResponse(assetResponse));

        return postResponse;
    }

    @Override
    public PageResponse<PostResponse> findAll(int page, int limit, String type) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepo.findAllByType(pageable, PostType.valueOf(type));

        List<PostResponse> posts = postPage.stream().
                map(p -> {
                    AssetResponse assetResponse = assetToPostServiceBlockingStub.getAssetDetail(AssetRequest.newBuilder()
                            .setId(p.getAssetId())
                            .build());

                    PostResponse postResponse = postMapper.convertToPostDetailResponse(p);
                    postResponse.setAssetDetail(assetMapper.convertToResponse(assetResponse));
                    return postResponse;
                })
                .collect(Collectors.toList());


        return PageResponse.<PostResponse>builder()
                .content(posts)
                .size(posts.size())
                .totalPages(postPage.getTotalPages())
                .totalElements(postPage.getTotalElements())
                .last(postPage.isLast())
                .build();
    }

    @Override
    public PageResponse<PostOverviewResponse> findAllByStatus(String status, String userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostOverview> postOverviews = postRepo.findAllByStatus(pageable, PostStatus.valueOf(status));

        List<PostOverviewResponse> data = postOverviews.get()
                .map(p -> {
                    PostOverviewResponse overviewResponse = postMapper.convertToResponse(p);
                    Content content = assetToPostServiceBlockingStub.getPoster(
                            AssetRequest.newBuilder()
                                    .setUserId(userId)
                                    .setId(p.getAssetId())
                                    .build()
                    );
                    overviewResponse.setPosterUrl(content.getUrl());
                    return overviewResponse;
                }).collect(Collectors.toList());


        return PageResponse.<PostOverviewResponse>builder()
                .content(data)
                .last(postOverviews.isLast())
                .totalPages(postOverviews.getTotalPages())
                .page(page)
                .size(postOverviews.getSize())
                .totalElements(postOverviews.getTotalElements())
                .build();
    }

    @Override
    @Transactional
    public MessageResponse createComment(String postId, Map<String, String> content) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        Comment comment = Comment.builder()
                .userId(content.get("userId"))
                .createdAt(LocalDateTime.now())
                .content(content.get("content"))
                .build();

        post.addComment(comment);

        comment = commentRepo.save(comment);
        return MessageResponse.builder()
                .message("Successfully create comment")
                .data(Map.of(
                        "id", comment.getId()
                ))
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse deleteComment(String postId, Map<String, String> content) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        List<Comment> comments = post.getComments().stream()
                .filter(c -> c.getId().equals(content.get("commentId")))
                .collect(Collectors.toList());

        if (comments.isEmpty())
            throw new ResourceNotFoundExeption("Not found comment");

        Comment comment = comments.get(0);

        if (!comment.getUserId().equals(content.get("userId")))
            throw new UnauthorizedException("Don't have permission for this resource");

        post.removeComment(comment);

        return MessageResponse.builder()
                .message("Successfully delete comment")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public PageResponse<CommentResponse> findCommentsByPostId(String postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepo.findAllByPostId(postId, pageable);
        List<CommentResponse> commentResponses = comments.get()
                .map(c -> CommentResponse.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(commentResponses)
                .size(comments.getSize())
                .last(comments.isLast())
                .totalElements(comments.getTotalElements())
                .page(comments.getNumber())
                .build();
    }

    @Override
    public List<StatusCountResponse> countStatusPost(String userId) {
        List<StatusCount> counts = postRepo.countStatusPost(userId);

        return counts.stream()
                .map(c -> StatusCountResponse.builder()
                        .count(c.getCount())
                        .name(c.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}

