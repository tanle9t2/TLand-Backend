package com.tanle.tland.user_service.service.impl;

import com.google.protobuf.ByteString;
import com.tanle.tland.upload_service.grpc.FileChunk;
import com.tanle.tland.upload_service.grpc.UploadResponse;
import com.tanle.tland.upload_service.grpc.UploadServiceGrpc;
import com.tanle.tland.user_service.entity.User;
import com.tanle.tland.user_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.user_service.mapper.UserMapper;
import com.tanle.tland.user_service.projection.UserLandingPage;
import com.tanle.tland.user_service.projection.UserProfile;
import com.tanle.tland.user_service.repo.UserRepo;
import com.tanle.tland.user_service.request.UserSignUpRequest;
import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.*;
import com.tanle.tland.user_service.service.UserService;
import com.tanle.tland.user_service.utils.TypeMedia;
import io.grpc.stub.StreamObserver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    @GrpcClient("uploadServiceGrpc")
    private UploadServiceGrpc.UploadServiceStub uploadServiceStub;

    @Override
    public UserInfo findUserById(String id) {
        User user = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundExeption("Not found user: " + id));

        return userMapper.convertToUserInfo(user);
    }

    @Override
    public UserLandingPageResponse findUserLandingPage(String userId) {
        UserLandingPage userLandingPage = userRepo.findById(userId, UserLandingPage.class)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        Long totalFollower = userRepo.countFollower(userId);
        Long totalFollowing = userRepo.countFlowing(userId);

        UserLandingPageResponse response = userMapper.convertToResponse(userLandingPage);
        response.setTotalFollower(totalFollower);
        response.setTotalFollowing(totalFollowing);

        return response;
    }

    @Override
    public MessageResponse checkUserFollow(String userId, String followerId) {
        Long isFollow = userRepo.checkFollow(userId, followerId);

        return MessageResponse.builder()
                .data(Map.of("isFollow", isFollow))
                .status(HttpStatus.OK)
                .message("SUCCESS")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse createUser(UserSignUpRequest request) {
        User user = User.builder()
                .id(request.getUserId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .createdAt(LocalDateTime.now())
                .email(request.getEmail())
                .isActive(true)
                .sex(true)
                .username(request.getUsername())
                .build();

        userRepo.save(user);
        return MessageResponse.builder()
                .data(request)
                .message("Successfully sign up")
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    public UserProfileResponse findProfileUser(String id) {
        UserProfile userProfile = userRepo.findProfileUser(id)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));

        return userMapper.convertToResponse(userProfile);

    }

    @Override
    public PageResponse<UserInfo> findAdd(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<User> userPage = userRepo.findAll(pageable);
        List<UserInfo> userInfoList = userPage.get()
                .map(u -> userMapper.convertToUserInfo(u))
                .collect(Collectors.toList());

        return PageResponse.<UserInfo>builder()
                .content(userInfoList)
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .page(userPage.getNumber())
                .size(userInfoList.size())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public MessageResponse updateProfile(String id, UserUpdateRequest request) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));

        userMapper.updateUserFromRequest(request, user);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully update profile")
                .data(user)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse inActiveUser(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        user.setActive(false);
        userRepo.save(user);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully inactive user")
                .build();
    }

    @Override
    @Transactional
    public void updateLastAccess(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        user.setLastAccess(LocalDateTime.now());
        userRepo.save(user);
    }

    @Override
    @Transactional
    public MessageResponse updateMedia(String userId, String type, MultipartFile file) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        final UploadResponse[] responses = new UploadResponse[1];
        CountDownLatch finishLatch = new CountDownLatch(1);

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
            Map<String, String> data = new HashMap<>();
            if (TypeMedia.AVT.name().equals(type)) {
                user.setAvtUrl(responses[0].getUrl());
                data.put("avtUrl", user.getAvtUrl());
            } else {
                user.setBannerUrl(responses[0].getUrl());
                data.put("bannerUrl", user.getBannerUrl());
            }

            userRepo.save(user);

            return MessageResponse.builder()
                    .status(HttpStatus.OK)
                    .message("Successfully updated avatar")
                    .data(data)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    @Transactional
    public MessageResponse followerUser(String userId, String followerId) {
        //follower id is people that click follow
        //user id is followed by follower
        Long checkFollow = userRepo.checkFollow(userId, followerId);
        if (checkFollow > 0) {
            throw new RuntimeException("User follow is existed");
        }
        userRepo.followUser(followerId, userId);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully unfollow user")
                .data(Map.of("followerId", followerId))
                .build();
    }

    @Override
    @Transactional
    public MessageResponse unfollowUser(String userId, String followerId) {
        Long checkFollow = userRepo.checkFollow(userId, followerId);
        if (checkFollow < 0) {
            throw new RuntimeException("User follow is not existed");
        }
        userRepo.unfollowUser(followerId, userId);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully unfollow user")
                .data(Map.of("followerId", followerId))
                .build();
    }

    @Override
    public PageResponse<FollowResponse> getFollower(String userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<User> pageFollower = userRepo.findFollowerByUserId(userId, pageable);

        List<FollowResponse> data = pageFollower.get()
                .map(u -> userMapper.convertToFollowResponse(u))
                .collect(Collectors.toList());


        return PageResponse.<FollowResponse>builder()
                .content(data)
                .totalPages(pageFollower.getTotalPages())
                .totalElements(pageFollower.getTotalElements())
                .page(pageFollower.getNumber())
                .size(data.size())
                .last(pageFollower.isLast())
                .build();
    }

    @Override
    public PageResponse<FollowResponse> getFollowing(String userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);

        Page<User> pageFollowing = userRepo.findFollowingByUserId(userId, pageable);

        List<FollowResponse> data = pageFollowing.get()
                .map(u -> userMapper.convertToFollowResponse(u))
                .collect(Collectors.toList());


        return PageResponse.<FollowResponse>builder()
                .content(data)
                .totalPages(pageFollowing.getTotalPages())
                .totalElements(pageFollowing.getTotalElements())
                .page(pageFollowing.getNumber())
                .size(data.size())
                .last(pageFollowing.isLast())
                .build();
    }
}
