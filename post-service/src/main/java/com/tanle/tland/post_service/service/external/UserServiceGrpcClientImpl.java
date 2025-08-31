package com.tanle.tland.post_service.service.external;

import com.tanle.tland.post_service.grpc.UserInfoRequest;
import com.tanle.tland.post_service.grpc.UserPostInfoResponse;
import com.tanle.tland.post_service.grpc.UserToPostServiceGrpc;
import com.tanle.tland.post_service.service.UserServiceGrpcClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceGrpcClientImpl implements UserServiceGrpcClient {
    @GrpcClient("userServiceGrpc")
    private UserToPostServiceGrpc.UserToPostServiceBlockingStub userToPostServiceBlockingStub;


    @Override
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "fallbackUser")
    @Retry(name = "userServiceRetry")
    public UserPostInfoResponse getUserInfo(UserInfoRequest request) {
        log.info("Calling userToPostService for userId={}", request.getId());
        return userToPostServiceBlockingStub.getUserInfo(request);
    }

    public UserPostInfoResponse fallbackUser(UserInfoRequest request, Throwable t) {
        return UserPostInfoResponse.newBuilder()
                .setId(request.getId())
                .setFirstName("Anonymous")
                .setLastName("Anonymous")
                .build();
    }
}
