package com.tanle.tland.post_service.mapper.decorator;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.grpc.UserInfoRequest;
import com.tanle.tland.post_service.grpc.UserPostInfoResponse;
import com.tanle.tland.post_service.grpc.UserToPostServiceGrpc;
import com.tanle.tland.post_service.mapper.AssetMapper;
import com.tanle.tland.post_service.mapper.PostMapper;
import com.tanle.tland.post_service.mapper.UserMapper;
import com.tanle.tland.post_service.response.PostDetailResponse;
import com.tanle.tland.post_service.service.UserServiceGrpcClient;
import com.tanle.tland.post_service.service.impl.AssetServiceGrpcClient;
import com.tanle.tland.user_serivce.grpc.AssetRequest;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import com.tanle.tland.user_serivce.grpc.AssetToPostServiceGrpc;
import lombok.NoArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;

@NoArgsConstructor
public abstract class PostMapperDecorator implements PostMapper {

    @Autowired
    private PostMapper delegate;
    @Autowired
    private AssetServiceGrpcClient assetServiceGrpcClient;
    @Autowired
    private UserServiceGrpcClient userServiceGrpcClient;
    @Autowired
    private AssetMapper assetMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public PostDetailResponse convertToResponse(Post post) {

        com.tanle.tland.post_service.response.PostDetailResponse postResponse = delegate.convertToResponse(post);
        AssetResponse assetResponse = assetServiceGrpcClient.getAssetDetail(AssetRequest.newBuilder()
                .setId(post.getAssetId())
                .build());

        UserPostInfoResponse userInfoResponse = userServiceGrpcClient.getUserInfo(UserInfoRequest.newBuilder()
                .setId(post.getUserId())
                .build());

        postResponse.setAssetDetail(assetMapper.convertToResponse(assetResponse));
        postResponse.setUserInfo(userMapper.convertToResponse(userInfoResponse));

        return postResponse;
    }
}
