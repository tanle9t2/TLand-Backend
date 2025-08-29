package com.tanle.tland.user_service.projection;

import java.time.LocalDateTime;

public interface UserLandingPage {
    String getId();

    String getDescription();

    LocalDateTime getCreatedAt();

    String getFirstName();

    String getLastName();

    String getAvtUrl();

    String getBannerUrl();


}
