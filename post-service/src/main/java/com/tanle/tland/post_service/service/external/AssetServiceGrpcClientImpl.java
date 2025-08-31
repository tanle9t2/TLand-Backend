package com.tanle.tland.post_service.service.external;

import com.tanle.tland.post_service.grpc.UserInfoRequest;
import com.tanle.tland.post_service.grpc.UserPostInfoResponse;
import com.tanle.tland.post_service.service.impl.AssetServiceGrpcClient;
import com.tanle.tland.user_serivce.grpc.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssetServiceGrpcClientImpl implements AssetServiceGrpcClient {
    @GrpcClient("assetServiceGrpc")
    private AssetToPostServiceGrpc.AssetToPostServiceBlockingStub assetToPostServiceBlockingStub;


    @CircuitBreaker(name = "assetServiceCB")
    @Retry(name = "assetServiceRetry")
    @Override
    public AssetResponse getAssetDetail(AssetRequest request) {
        return assetToPostServiceBlockingStub.getAssetDetail(request);
    }

    @Override
    public CheckExistedResponse checkExisted(AssetRequest request) {
        return assetToPostServiceBlockingStub.checkExisted(request);
    }

    @CircuitBreaker(name = "assetServiceCB",fallbackMethod = "fallbackGetPoster")
    @Retry(name = "assetServiceRetry")
    @Override
    public Content getPoster(AssetRequest request) {
        return assetToPostServiceBlockingStub.getPoster(request);
    }

    public Content fallbackGetPoster(AssetRequest request, Throwable throwable) {
        log.error("Fallback triggered for {} due to {}", request.getId(), throwable.toString());
        return Content.newBuilder()
                .build();
    }

}
