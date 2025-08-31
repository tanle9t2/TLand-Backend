package com.tanle.tland.user_service.service;

import com.tanle.tland.user_service.request.UserSignUpRequest;
import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserInfo findUserById(String id);

    UserLandingPageResponse findUserLandingPage(String userId);

    MessageResponse checkUserFollow(String userId, String followerId);

    MessageResponse createUser(UserSignUpRequest request);

    UserProfileResponse findProfileUser(String id, HttpServletRequest httpServletRequest);

    PageResponse<UserInfo> findAdd(int page, int limit);

    MessageResponse updateProfile(String id, UserUpdateRequest request);

    MessageResponse inActiveUser(String userId);

    void updateLastAccess(String userId);

    MessageResponse updateMedia(String userId, String type, MultipartFile file);

    MessageResponse followerUser(String userId, String followerId);

    MessageResponse unfollowUser(String userId, String followerId);

    PageResponse<FollowResponse> getFollower(String userId, int page, int limit);

    PageResponse<FollowResponse> getFollowing(String userId, int page, int limit);

}
