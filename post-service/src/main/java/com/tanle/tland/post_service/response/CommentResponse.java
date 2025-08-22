package com.tanle.tland.post_service.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CommentResponse {
    private String id;
    private String content;
    private UserInfo userInfo;
    private LocalDateTime createdAt;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class UserInfo {
        private String id;
        private String firstName;
        private String lastName;
        private String avtUrl;
    }
}
