package com.tanle.tland.asset_service.service.impl;

import com.google.protobuf.ByteString;
import com.tanle.tland.asset_service.entity.*;
import com.tanle.tland.asset_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.asset_service.mapper.AssetMapper;
import com.tanle.tland.asset_service.projection.AssetSummary;
import com.tanle.tland.asset_service.repo.AssetRepo;
import com.tanle.tland.asset_service.repo.CategoryRepo;
import com.tanle.tland.asset_service.repo.ProjectRepo;
import com.tanle.tland.asset_service.request.AssetCreateRequest;
import com.tanle.tland.asset_service.request.UploadImageRequest;
import com.tanle.tland.asset_service.response.AssetDetailResponse;
import com.tanle.tland.asset_service.response.AssetSummaryResponse;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.AssetService;
import com.tanle.tland.asset_service.service.PostServiceGrpcClient;
import com.tanle.tland.asset_service.service.ProjectService;
import com.tanle.tland.post_service.grpc.PostCheckAttachRequest;
import com.tanle.tland.post_service.grpc.PostCheckAttachResponse;
import com.tanle.tland.upload_service.grpc.FileChunk;
import com.tanle.tland.upload_service.grpc.UploadResponse;
import com.tanle.tland.upload_service.grpc.UploadServiceGrpc;
import com.tanle.tland.user_serivce.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.catalina.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {
    @GrpcClient("userServiceGrpc")
    private UserToAssetServiceGrpc.UserToAssetServiceBlockingStub serviceBlockingStub;
    @GrpcClient("uploadServiceGrpc")
    private UploadServiceGrpc.UploadServiceStub uploadServiceStub;
    private final PostServiceGrpcClient postServiceGrpcClient;
    private final AssetRepo assetRepo;
    private final AssetMapper assetMapper;
    private final ProjectRepo projectRepo;

    @Override
    public AssetDetailResponse findAssetById(String id) {
        Asset asset = assetRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found asset"));
        PostCheckAttachResponse postCheckAttachResponse = postServiceGrpcClient.checkAttachedPost(
                PostCheckAttachRequest.newBuilder()
                        .setId(asset.getId())
                        .build()
        );
        AssetDetailResponse response = assetMapper.convertToDetailResponse(asset);
        response.setAttachedPostShow(postCheckAttachResponse.getIsAttached());
        return response;
    }

    @Override
    public List<AssetDetailResponse> findAssetByType(String type, String userId) {
        List<AssetDetailResponse> assetDetailResponses = assetRepo.findAllByTypeAndUserId(AssetType.valueOf(type), userId)
                .stream()
                .map(a -> assetMapper.convertToDetailResponse(a))
                .collect(Collectors.toList());

        return assetDetailResponses;
    }

    @Override
    public PageResponse<AssetSummaryResponse> findAll(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AssetSummary> assetPage = assetRepo.findAllByUserIdAndType(userId, AssetType.PERSIST, pageable);
        List<AssetSummaryResponse> data = assetPage.get()
                .map(a -> {
                    AssetSummaryResponse response = assetMapper.convertToAssetSummaryResponse(a);
                    PostCheckAttachResponse isAttached = postServiceGrpcClient.checkAttachedPost(PostCheckAttachRequest.newBuilder()
                            .setId(a.getId())
                            .build());

                    response.setAttachedPost(isAttached.getIsAttached());
                    return response;
                })
                .collect(Collectors.toList());

        return PageResponse.<AssetSummaryResponse>builder()
                .last(assetPage.isLast())
                .totalElements(assetPage.getTotalElements())
                .content(data)
                .totalPages(assetPage.getTotalPages())
                .page(page)
                .size(assetPage.getSize())
                .build();
    }

    @Override
    public MessageResponse deleteAsset(String assetId, String userId) throws AccessDeniedException {
        Asset asset = assetRepo.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found asset"));

        if (!asset.getUserId().equals(userId))
            throw new AccessDeniedException("Don't have permission for this resource");

        PostCheckAttachResponse response = postServiceGrpcClient.checkAttachedPost(
                PostCheckAttachRequest.newBuilder()
                        .setId(asset.getId())
                        .build()
        );
        if (response.getIsAttached())
            throw new RuntimeException("Asset is showing");


        asset.setType(AssetType.DELETE);
        assetRepo.save(asset);

        return MessageResponse.builder()
                .message("Successfully delete asset")
                .status(HttpStatus.OK)
                .data(Map.of("id", asset.getId()))
                .build();
    }

    @Override
    public MessageResponse linkAssetToProject(String assetId, String projectId) {
        projectRepo.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found project: " + projectId));

        Asset asset = assetRepo.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found asset"));

        asset.setProjectId(projectId);
        assetRepo.save(asset);
        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully link asset to project")
                .build();
    }

    @Override
    public MessageResponse createAsset(AssetCreateRequest createRequest, String userId) {
        UserResponse userResponse = serviceBlockingStub.getUserById(UserRequest.newBuilder()
                .setId(userId)
                .build());


        Asset asset = assetMapper.convertToAsset(createRequest);
        asset.getContents().forEach(c -> c.generateId());
        asset.setId(UUID.randomUUID().toString());
        assetRepo.save(asset);
        return MessageResponse.builder()
                .status(HttpStatus.CREATED)
                .message("Successfully create asset")
                .build();
    }

    @Override
    public MessageResponse uploadMedia(String userId, UploadImageRequest request) {
        Asset asset;
        if (request.getAssetId() == null) {
            asset = Asset.builder()
                    .userId(userId)
                    .id(UUID.randomUUID().toString())
                    .build();
        } else
            asset = assetRepo.findById(request.getAssetId())
                    .orElseThrow(() -> new ResourceNotFoundExeption("Not found asset"));
        final UploadResponse[] responses = new UploadResponse[1];
        MultipartFile file = request.getFile();
        CountDownLatch finishLatch = new CountDownLatch(1);
        Map<String, String> response = new HashMap<>();
        StreamObserver<UploadResponse> uploadResponseStreamObserver = new StreamObserver<UploadResponse>() {
            @Override
            public void onNext(UploadResponse uploadResponse) {
                responses[0] = uploadResponse;
            }

            @Override
            public void onError(Throwable throwable) {
                finishLatch.countDown();
                throw new RuntimeException(throwable);
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };
        StreamObserver<FileChunk> requestObserver = uploadServiceStub.upload(uploadResponseStreamObserver);

        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[1024 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                FileChunk chunk = FileChunk.newBuilder()
                        .setContent(ByteString.copyFrom(buffer, 0, bytesRead))
                        .setFileName(file.getOriginalFilename())
                        .build();
                requestObserver.onNext(chunk);
            }
            requestObserver.onCompleted();
            if (!finishLatch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Upload timed out");
            }
            if (responses[0] == null) {
                throw new RuntimeException("No response received from upload service");
            }
            UploadResponse uploadResponse = responses[0];

            if (uploadResponse.getType().equals(ContentType.IMAGE.name())) {
                Image image = Image.builder()
                        .id(UUID.randomUUID().toString())
                        .url(uploadResponse.getUrl())
                        .name(file.getOriginalFilename())
                        .createdAt(LocalDateTime.now())
                        .build();
                asset.addContent(image);
            } else {
                Video video = Video.builder()
                        .id(UUID.randomUUID().toString())
                        .url(uploadResponse.getUrl())
                        .name(file.getOriginalFilename())
                        .duration(uploadResponse.getDuration())
                        .createdAt(LocalDateTime.now())
                        .build();
                asset.addContent(video);
            }
            response.put("assetId", asset.getId());
            response.put("url", uploadResponse.getUrl());
            assetRepo.save(asset);

            return MessageResponse.builder()
                    .status(HttpStatus.OK)
                    .message("Successfully upload image")
                    .data(response)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MessageResponse updateAsset(AssetCreateRequest request, String userId) throws AccessDeniedException {
        Asset asset = assetRepo.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found asset"));
        PostCheckAttachResponse response = postServiceGrpcClient.checkAttachedPost(
                PostCheckAttachRequest.newBuilder()
                        .setId(asset.getId())
                        .build()
        );
        if (response.getIsAttached())
            throw new RuntimeException("Asset is attaching post");


        if (!asset.getUserId().equals(userId))
            throw new AccessDeniedException("Don't have permission for this resource");

        assetMapper.updateConvertAsset(request, asset);
        assetRepo.save(asset);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully update asset")
                .build();
    }
}
