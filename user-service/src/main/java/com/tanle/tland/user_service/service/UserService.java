package com.tanle.tland.user_service.service;

import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.FollowResponse;
import com.tanle.tland.user_service.response.MessageResponse;
import com.tanle.tland.user_service.response.PageResponse;
import com.tanle.tland.user_service.response.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserInfo findUserById(String id);

    PageResponse<UserInfo> findAdd(int page, int limit);

    MessageResponse updateProfile(String id, UserUpdateRequest request);

    MessageResponse inActiveUser(String userId);

    void updateLastAccess(String userId);

    MessageResponse updateAvt(String userId, MultipartFile file);

    FollowResponse followerUser(String userId, String followerId);

    MessageResponse unfollowUser(String userId, String followerId);

    PageResponse<FollowResponse> getFollower(String userId, int page, int limit);

    PageResponse<FollowResponse> getFollowing(String userId, int page, int limit);

}
