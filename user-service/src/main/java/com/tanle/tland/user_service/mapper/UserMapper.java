package com.tanle.tland.user_service.mapper;

import com.google.protobuf.Timestamp;

import com.tanle.tland.user_serivce.grpc.UserPostInfoResponse;
import com.tanle.tland.user_service.entity.User;
import com.tanle.tland.user_service.projection.UserLandingPage;
import com.tanle.tland.user_service.projection.UserPostInfo;
import com.tanle.tland.user_service.projection.UserProfile;
import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.FollowResponse;
import com.tanle.tland.user_service.response.UserInfo;
import com.tanle.tland.user_service.response.UserLandingPageResponse;
import com.tanle.tland.user_service.response.UserProfileResponse;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Mapper(componentModel = "spring")
public interface UserMapper {
    UserInfo convertToUserInfo(User user);

    UserProfileResponse convertToResponse(UserProfile userProfile);

    UserLandingPageResponse convertToResponse(UserLandingPage userLandingPage);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);

    @Mapping(source = "createdAt", target = "createdAt")
    UserPostInfoResponse convertToGrpcResponse(UserPostInfo user);

    FollowResponse convertToFollowResponse(User user);

    default Timestamp map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Timestamp.newBuilder()
                .setSeconds(localDateTime.toEpochSecond(ZoneOffset.UTC))
                .setNanos(localDateTime.getNano())
                .build();
    }
}
