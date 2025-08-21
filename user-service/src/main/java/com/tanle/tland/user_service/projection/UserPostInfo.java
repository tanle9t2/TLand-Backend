package com.tanle.tland.user_service.projection;

import java.time.LocalDateTime;

public interface UserPostInfo {
    String getId();
    String getFirstName();

    String getLastName();

    String getAvtUrl();
    String getEmail();
    LocalDateTime getCreatedAt();
    LocalDateTime getLastAccess();
    String getPhoneNumber();

}
