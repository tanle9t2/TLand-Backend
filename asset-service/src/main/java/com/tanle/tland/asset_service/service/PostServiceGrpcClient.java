package com.tanle.tland.asset_service.service;


import com.tanle.tland.post_service.grpc.PostCheckAttachRequest;
import com.tanle.tland.post_service.grpc.PostCheckAttachResponse;

public interface PostServiceGrpcClient {
    PostCheckAttachResponse checkAttachedPost(PostCheckAttachRequest request);
}
