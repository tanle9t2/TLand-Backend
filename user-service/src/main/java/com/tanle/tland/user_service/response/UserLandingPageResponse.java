package com.tanle.tland.user_service.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLandingPageResponse {
    private String id;
    private String avtUrl;
    private String bannerUrl;
    private String description;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
    private Long totalFollower;
    private Long totalFollowing;
}
