package com.tanle.tland.user_service.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    private String username;
    private String email;
    private String description;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String phoneNumber;
    private String taxCode;
    private String cid;
    private boolean sex;
}
