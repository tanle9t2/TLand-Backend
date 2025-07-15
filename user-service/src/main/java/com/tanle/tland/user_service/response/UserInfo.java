package com.tanle.tland.user_service.response;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {
    private String id;

    private String username;

    private String password;

    private String email;

    private String firstName;

    private String lastName;

    private LocalDate dob;

    private LocalDateTime createdAt;

    private LocalDateTime lastAccess;

    private boolean isActive;

    private boolean sex;

    private String avtUrl;
}
