package com.tanle.tland.post_service.service.impl;

import com.tanle.tland.user_serivce.grpc.AssetRequest;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import com.tanle.tland.user_serivce.grpc.CheckExistedResponse;
import com.tanle.tland.user_serivce.grpc.Content;

public interface AssetServiceGrpcClient {
    AssetResponse getAssetDetail(AssetRequest request);

    CheckExistedResponse checkExisted(AssetRequest request);

    Content getPoster(AssetRequest request);
}
