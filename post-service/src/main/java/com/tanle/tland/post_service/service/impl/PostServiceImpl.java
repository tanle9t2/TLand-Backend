package com.tanle.tland.post_service.service.impl;


import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.mapper.PostMapper;
import com.tanle.tland.post_service.repo.PostRepo;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.MessageResponse;
import com.tanle.tland.post_service.service.PostService;
import com.tanle.tland.user_serivce.grpc.AssetRequest;
import com.tanle.tland.user_serivce.grpc.AssetToPostServiceGrpc;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        postRepo.save(post);

        return MessageResponse.builder()
                .message("Successfully create post")
                .status(HttpStatus.CREATED)
                .build();
    }
}
