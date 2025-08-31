package com.tanle.tland.asset_service.service.external;

import com.tanle.tland.asset_service.service.PostServiceGrpcClient;

import com.tanle.tland.post_service.grpc.PostCheckAttachRequest;
import com.tanle.tland.post_service.grpc.PostCheckAttachResponse;
import com.tanle.tland.post_service.grpc.PostToAssetServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PostServiceGrpcClientImpl implements PostServiceGrpcClient {
    @GrpcClient("postServiceGrpc")
    private PostToAssetServiceGrpc.PostToAssetServiceBlockingStub postToAssetServiceBlockingStub;

    @Override
    @Retry(name = "postServiceRetry")
    @CircuitBreaker(name = "postServiceCB", fallbackMethod = "fallbackABC")
    public PostCheckAttachResponse checkAttachedPost(PostCheckAttachRequest request) {
        log.error("Retry...");

        return postToAssetServiceBlockingStub.checkAttachedPost(request);
    }

    // fallback triggered by Resilience4j
    public PostCheckAttachResponse fallbackABC(PostCheckAttachRequest request, Throwable t) {
        log.error("Fallback triggered for {} due to {}", request.getId(), t.toString());
        return PostCheckAttachResponse.newBuilder().setIsAttached(false).build();
    }

}
