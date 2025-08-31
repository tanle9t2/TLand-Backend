package com.tanle.tland.user_service.service.impl;

import com.tanle.tland.user_serivce.grpc.UserInfoRequest;

import com.tanle.tland.user_serivce.grpc.UserPostInfoResponse;
import com.tanle.tland.user_serivce.grpc.UserToPostServiceGrpc;
import com.tanle.tland.user_service.entity.User;
import com.tanle.tland.user_service.mapper.UserMapper;
import com.tanle.tland.user_service.projection.UserPostInfo;
import com.tanle.tland.user_service.repo.UserRepo;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;

@GrpcService
@RequiredArgsConstructor
public class UserToPostServiceGrpcImpl extends UserToPostServiceGrpc.UserToPostServiceImplBase {
    private final UserRepo userRepo;
    private final UserMapper userMapper;

    @Override
    public void getUserInfo(UserInfoRequest request, StreamObserver<UserPostInfoResponse> responseObserver) {
        Optional<UserPostInfo> userOptional = userRepo.findById(request.getId(), UserPostInfo.class);
        if (userOptional.isPresent()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found with ID: " + request.getId())
                            .asRuntimeException());
            return;
        }
        UserPostInfo user = userOptional.get();

        UserPostInfoResponse response = userMapper.convertToGrpcResponse(user);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
