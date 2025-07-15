package com.tanle.tland.user_service.mapper.decorator;

import com.tanle.tland.user_service.entity.User;
import com.tanle.tland.user_service.mapper.UserMapper;
import com.tanle.tland.user_service.response.FollowResponse;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class UserMapperDecorator implements UserMapper {
    @Autowired
    private UserMapper delegate;


}
