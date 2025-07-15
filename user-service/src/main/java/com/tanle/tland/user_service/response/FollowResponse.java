package com.tanle.tland.user_service.response;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String avtUrl;
}
