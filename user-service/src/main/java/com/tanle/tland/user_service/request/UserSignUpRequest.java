package com.tanle.tland.user_service.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSignUpRequest {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
}
