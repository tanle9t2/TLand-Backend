package com.tanle.tland.user_service.controller;

import com.tanle.tland.user_service.projection.UserProfile;
import com.tanle.tland.user_service.request.UserSignUpRequest;
import com.tanle.tland.user_service.request.UserUpdateRequest;
import com.tanle.tland.user_service.response.*;
import com.tanle.tland.user_service.service.UserService;
import com.tanle.tland.user_service.service.impl.KeycloakService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final KeycloakService keycloakService;

    @PostMapping("/user/sign-up")
    public ResponseEntity<MessageResponse> signUpUser(@RequestBody UserSignUpRequest request) {
        keycloakService.createUser(request);
        MessageResponse messageResponse = userService.createUser(request);

        return ResponseEntity.ok(messageResponse);
    }

    @GetMapping("/user")
    public ResponseEntity<UserInfo> getUserById(@RequestHeader("X-UserId") String id) {
        UserInfo response = userService.findUserById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/profile")
    public ResponseEntity<UserProfileResponse> getProfileUser(
            HttpServletRequest httpServletRequest,
            @RequestHeader("X-UserId") String id) {
        UserProfileResponse response = userService.findProfileUser(id,httpServletRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/user/page/{userId}")
    public ResponseEntity<UserLandingPageResponse> getUserLandingPage(@PathVariable("userId") String userId) {
        UserLandingPageResponse response = userService.findUserLandingPage(userId);
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

    @PutMapping("/user")
    public ResponseEntity<MessageResponse> updateProfile(
            @RequestHeader("X-UserId") String id,
            @RequestBody UserUpdateRequest request) {
        MessageResponse response = userService.updateProfile(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user/{userId}/last-access")
    public ResponseEntity<Void> updateLastAccess(
            @RequestHeader("X-UserId") String id) {
        userService.updateLastAccess(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/user/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> updateMedia(
            @RequestHeader("X-UserId") String id,
            @RequestParam(name = "type") String type,
            @RequestParam MultipartFile file) {
        MessageResponse response = userService.updateMedia(id, type, file);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/user/{userId}/deactivate")
    public ResponseEntity<Void> inActiveUser(@PathVariable("userId") String id) {
        userService.inActiveUser(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/follow/{followerId}")
    public ResponseEntity<MessageResponse> checkUserFollow(
            @RequestHeader("X-UserId") String id,
            @PathVariable("followerId") String followerId) {
        MessageResponse response = userService.checkUserFollow(id, followerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/user/unfollow/{followerId}")
    public ResponseEntity<MessageResponse> unfollowUser(
            @RequestHeader("X-UserId") String id,
            @PathVariable("followerId") String followerId) {
        MessageResponse response = userService.unfollowUser(id, followerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/user/follow/{followerId}")
    public ResponseEntity<MessageResponse> followUser(
            @RequestHeader("X-UserId") String id,
            @PathVariable("followerId") String followerId) {
        MessageResponse response = userService.followerUser(id, followerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/follower")
    public ResponseEntity<PageResponse<FollowResponse>> getFollower(
            @RequestHeader("X-UserId") String id,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "limit", defaultValue = PAGE_SIZE) String limit) {
        PageResponse<FollowResponse> response = userService.getFollower(id, Integer.parseInt(page), Integer.parseInt(limit));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/following")
    public ResponseEntity<PageResponse<FollowResponse>> getFollowing(
            @RequestHeader("X-UserId") String id,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "limit", defaultValue = PAGE_SIZE) String limit) {
        PageResponse<FollowResponse> response = userService.getFollowing(id, Integer.parseInt(page), Integer.parseInt(limit));
        return ResponseEntity.ok(response);
    }
}
