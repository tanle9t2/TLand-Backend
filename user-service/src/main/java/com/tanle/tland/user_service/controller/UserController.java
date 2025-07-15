package com.tanle.tland.user_service.controller;

import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.FollowResponse;
import com.tanle.tland.user_service.response.MessageResponse;
import com.tanle.tland.user_service.response.PageResponse;
import com.tanle.tland.user_service.response.UserInfo;
import com.tanle.tland.user_service.service.UserService;
import jakarta.ws.rs.GET;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.tanle.tland.user_service.utils.AppConstant.*;

@RestController
@RequestMapping(value = "/api/v1")

public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserInfo> getUserById(@PathVariable("userId") String id) {
        UserInfo response = userService.findUserById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserInfo>> getAll(
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "limit", defaultValue = PAGE_SIZE) String limit
    ) {
        PageResponse pageResponse = userService.findAdd(Integer.parseInt(page), Integer.parseInt(limit));
        return ResponseEntity.ok(pageResponse);
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<MessageResponse> updateProfile(
            @PathVariable("userId") String id,
            @RequestBody UserUpdateRequest request) {
        MessageResponse response = userService.updateProfile(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user/{userId}/last-access")
    public ResponseEntity<Void> updateLastAccess(
            @PathVariable("userId") String id) {
        userService.updateLastAccess(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/user/{userId}/update-avt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> updateAvt(
            @PathVariable("userId") String id,
            @RequestParam MultipartFile file) {
        MessageResponse response = userService.updateAvt(id, file);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/user/{userId}/deactivate")
    public ResponseEntity<Void> inActiveUser(@PathVariable("userId") String id) {
        userService.inActiveUser(id);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/user/{userId}/unfollow/{followerId}")
    public ResponseEntity<MessageResponse> unfollowUser(
            @PathVariable("userId") String id,
            @PathVariable("followerId") String followerId) {
        MessageResponse response = userService.unfollowUser(id, followerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/user/{userId}/follow/{followerId}")
    public ResponseEntity<FollowResponse> followUser(
            @PathVariable("userId") String id,
            @PathVariable("followerId") String followerId) {
        FollowResponse response = userService.followerUser(id, followerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/follower")
    public ResponseEntity<PageResponse<FollowResponse>> getFollower(
            @PathVariable("userId") String id,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "limit", defaultValue = PAGE_SIZE) String limit) {
        PageResponse<FollowResponse> response = userService.getFollower(id, Integer.parseInt(page), Integer.parseInt(limit));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/following")
    public ResponseEntity<PageResponse<FollowResponse>> getFollowing(
            @PathVariable("userId") String id,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "limit", defaultValue = PAGE_SIZE) String limit) {
        PageResponse<FollowResponse> response = userService.getFollowing(id, Integer.parseInt(page), Integer.parseInt(limit));
        return ResponseEntity.ok(response);
    }
}
