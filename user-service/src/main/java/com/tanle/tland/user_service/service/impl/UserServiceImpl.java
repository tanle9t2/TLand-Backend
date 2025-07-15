package com.tanle.tland.user_service.service.impl;

import com.tanle.tland.user_service.entity.User;
import com.tanle.tland.user_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.user_service.mapper.UserMapper;
import com.tanle.tland.user_service.repo.UserRepo;
import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.FollowResponse;
import com.tanle.tland.user_service.response.MessageResponse;
import com.tanle.tland.user_service.response.PageResponse;
import com.tanle.tland.user_service.response.UserInfo;
import com.tanle.tland.user_service.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final UserMapper userMapper;

    @Override
    public UserInfo findUserById(String id) {
        User user = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundExeption("Not found user: " + id));

        return userMapper.convertToUserInfo(user);
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
    public MessageResponse updateAvt(String userId, MultipartFile file) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));

        //call update service
//        user.setAvtUrl(avt);
        userRepo.save(user);


        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully update avatar")
//                .data(avt)
                .build();
    }

    @Override
    @Transactional
    public FollowResponse followerUser(String userId, String followerId) {
        //follower id is people that click follow
        //user id is followed by follower
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        User follower = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found follower"));

        userRepo.followUser(followerId, userId);

        return userMapper.convertToFollowResponse(user);
    }

    @Override
    @Transactional
    public MessageResponse unfollowUser(String userId, String followerId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        User follower = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found follower"));

        userRepo.unfollowUser(followerId, userId);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully unfollow user")
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
