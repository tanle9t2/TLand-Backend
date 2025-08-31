package com.tanle.tland.post_service.service;

import com.tanle.tland.post_service.grpc.UserInfoRequest;
import com.tanle.tland.post_service.grpc.UserPostInfoResponse;

public interface UserServiceGrpcClient {
    UserPostInfoResponse getUserInfo(UserInfoRequest request);
}
