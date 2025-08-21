package com.tanle.tland.post_service.mapper;

import com.google.protobuf.Timestamp;
import com.tanle.tland.post_service.grpc.UserPostInfoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface UserMapper {
    com.tanle.tland.post_service.response.UserPostInfoResponse convertToResponse(UserPostInfoResponse user);
    default LocalDateTime map(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos(), ZoneOffset.UTC);
    }
}
