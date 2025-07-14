package com.tanle.tland.user_service.service;

import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.MessageResponse;
import com.tanle.tland.user_service.response.PageResponse;
import com.tanle.tland.user_service.response.UserInfo;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserInfo findUserById(String id);

    PageResponse<UserInfo> findAdd(int page, int limit);

    MessageResponse updateProfile(String id, UserUpdateRequest request);

    MessageResponse inActiveUser(String userId);

    void updateLastAccess(String userId);

    MessageResponse updateAvt(String userId, MultipartFile file);

}
