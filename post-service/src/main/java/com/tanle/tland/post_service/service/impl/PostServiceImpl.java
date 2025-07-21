package com.tanle.tland.post_service.service.impl;


import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.entity.PostStatus;
import com.tanle.tland.post_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.post_service.exception.UnauthorizedException;
import com.tanle.tland.post_service.mapper.PostMapper;
import com.tanle.tland.post_service.repo.PostRepo;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.AssetDetailResponse;
import com.tanle.tland.post_service.response.MessageResponse;
import com.tanle.tland.post_service.response.PostResponse;
import com.tanle.tland.post_service.service.PostService;
import com.tanle.tland.user_serivce.grpc.AssetRequest;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import com.tanle.tland.user_serivce.grpc.AssetToPostServiceGrpc;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    @GrpcClient("assetService")
    private AssetToPostServiceGrpc.AssetToPostServiceBlockingStub assetToPostServiceBlockingStub;
    private final PostRepo postRepo;
    private final PostMapper postMapper;

    @Override
    @Transactional
    public MessageResponse createPost(PostCreateRequest request) {
        assetToPostServiceBlockingStub.checkExisted(AssetRequest.newBuilder()
                .setId(request.getAssetId())
                .setUserId(request.getUserId())
                .build());

        Post post = postMapper.convertToEntity(request);
        post.setStatus(PostStatus.CREATED);

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
    public PostResponse findPostById(String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        AssetResponse assetResponse = assetToPostServiceBlockingStub.getAssetDetail(AssetRequest.newBuilder()
                .setId(post.getAssetId())
                .build());

        PostResponse postResponse = postMapper.convertToPostDetailResponse(post);
        postResponse.setAssetDetail(
                AssetDetailResponse.builder()
                        .id(assetResponse.getId())
                        .dimension(assetResponse.getDimensionList())
                        .name(assetResponse.getName())
                        .address(assetResponse.getAddress())
                        .ward(assetResponse.getWard())
                        .province(assetResponse.getProvince())
                        .properties(assetResponse.getPropertiesMap())
                        .contents(
                                assetResponse.getContentListList().stream()
                                        .map(c -> AssetDetailResponse.Content.builder()
                                                .url(c.getUrl())
                                                .id(c.getId())
                                                .name(c.getName())
                                                .duration(c.getDuration())
                                                .type(c.getType())
                                                .build())
                                        .collect(Collectors.toList())
                        )
                        .build()
        );


        return postResponse;
    }
}

