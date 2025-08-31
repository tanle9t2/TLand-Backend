package com.tanle.tland.post_service.service.impl;

import com.tanle.tland.post_service.entity.PostStatus;
import com.tanle.tland.post_service.repo.PostRepo;
import com.tanle.tland.user_serivce.grpc.PostCheckAttachRequest;
import com.tanle.tland.user_serivce.grpc.PostCheckAttachResponse;
import com.tanle.tland.user_serivce.grpc.PostToAssetServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class PostServiceGrpcImpl extends PostToAssetServiceGrpc.PostToAssetServiceImplBase {
    private final PostRepo postRepo;

    @Override
    public void checkAttachedPost(PostCheckAttachRequest request, StreamObserver<PostCheckAttachResponse> responseObserver) {
        boolean isAttached = postRepo.existsByAssetIdAndStatusIn(request.getId(),
                List.of(PostStatus.SHOW, PostStatus.HIDE, PostStatus.WAITING_PAYMENT, PostStatus.WAITING_ACCEPT));
        PostCheckAttachResponse response = PostCheckAttachResponse.newBuilder()
                .setIsAttached(isAttached)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
