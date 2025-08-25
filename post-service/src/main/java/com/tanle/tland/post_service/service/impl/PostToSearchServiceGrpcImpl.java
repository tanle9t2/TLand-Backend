package com.tanle.tland.post_service.service.impl;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.mapper.PostMapper;
import com.tanle.tland.post_service.repo.PostRepo;
import com.tanle.tland.post_service.response.PostResponse;
import com.tanle.tland.post_service.service.PostService;
import com.tanle.tland.user_serivce.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class PostToSearchServiceGrpcImpl extends PostToSearchServiceGrpc.PostToSearchServiceImplBase {
    private final PostRepo postRepo;
    private final PostMapper postMapper;
    @GrpcClient("assetServiceGrpc")
    private AssetToPostServiceGrpc.AssetToPostServiceBlockingStub assetToPostServiceBlockingStub;

    @Override
    public void getPostById(PostDetailRequest request, StreamObserver<PostDetailResponse> responseObserver) {
        Optional<Post> optionalPost = postRepo.findById(request.getId());
        if (!optionalPost.isPresent()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Post not found with ID: " + request.getId())
                            .asRuntimeException());
        }
        Post post = optionalPost.get();
        AssetResponse assetResponse = assetToPostServiceBlockingStub.getAssetDetail(AssetRequest.newBuilder()
                .setId(post.getAssetId())
                .build());

        PostDetailResponse postDetailResponse = postMapper.convertToResponseGrpc(post).toBuilder()
                .setAssetDetail(assetResponse)
                .build();


        responseObserver.onNext(postDetailResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getAllPost(Empty request, StreamObserver<PostDetailResponseList> responseObserver) {
        List<PostDetailResponse> postDetailResponses = postRepo.findAll().stream()
                .map(p -> {
                    AssetResponse assetResponse = assetToPostServiceBlockingStub.getAssetDetail(AssetRequest.newBuilder()
                            .setId(p.getAssetId())
                            .build());
                    PostDetailResponse postDetailResponse = postMapper.convertToResponseGrpc(p).toBuilder()
                            .setAssetDetail(assetResponse)
                            .build();

                    return postDetailResponse;
                })
                .collect(Collectors.toList());

        PostDetailResponseList responseList = PostDetailResponseList.newBuilder()
                .addAllResponse(postDetailResponses)
                .build();

        responseObserver.onNext(responseList);
        responseObserver.onCompleted();
    }
}
