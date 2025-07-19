package com.tanle.tland.user_service.service.impl;

import com.tanle.tland.user_serivce.grpc.UserRequest;
import com.tanle.tland.user_serivce.grpc.UserResponse;
import com.tanle.tland.user_serivce.grpc.UserToAssetServiceGrpc;
import com.tanle.tland.user_service.entity.User;
import com.tanle.tland.user_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.user_service.repo.UserRepo;
import com.tanle.tland.user_service.service.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;


@GrpcService
@RequiredArgsConstructor
public class UserServiceGrpcImpl extends UserToAssetServiceGrpc.UserToAssetServiceImplBase {

    private final UserRepo userRepo;

    @Override
    public void getUserById(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        Optional<User> userOptional = userRepo.findById(request.getId());
        if (!userOptional.isPresent()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found with ID: " + request.getId())
                            .asRuntimeException());
            return;
        }
        User user = userOptional.get();
        responseObserver.onNext(UserResponse.newBuilder()
                .setId(user.getId())
                .build());
        responseObserver.onCompleted();
    }
}
