package com.tanle.tland.user_service.mapper;

import com.tanle.tland.user_service.entity.User;
import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.FollowResponse;
import com.tanle.tland.user_service.response.UserInfo;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface UserMapper {
    UserInfo convertToUserInfo(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);

    FollowResponse convertToFollowResponse(User user);
}
