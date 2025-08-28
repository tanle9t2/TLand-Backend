package com.tanle.tland.post_service.controller;

import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.*;
import com.tanle.tland.post_service.service.PostService;
import com.tanle.tland.post_service.response.PostDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

import static com.tanle.tland.post_service.utils.AppConstant.*;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping("/public/post/{postId}")
    public ResponseEntity<PostDetailResponse> getPostByID(@PathVariable("postId") String postId) {
        PostDetailResponse response = postService.findPostById(postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/posts")
    public ResponseEntity<PageResponse> getPosts(
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size,
            @RequestParam(value = "type") String type) {
        PageResponse response = postService.findAll(Integer.parseInt(page), Integer.parseInt(size), type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/post/admin")
    public ResponseEntity<PageResponse<PostAdminOverviewResponse>> getPostsByStatus(
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size,
            @RequestParam(value = "orderBy", required = false, defaultValue = ORDER_FIELD_DEFAULT) String orderBy,
            @RequestParam(value = "orderDirection", required = false, defaultValue = ORDER_DIRECTION_DEFAULT) String orderDirection,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "kw", defaultValue = "") String keyword) {
        PageResponse<PostAdminOverviewResponse> response = postService.findAllByStatus(status, keyword,
                Integer.parseInt(page), Integer.parseInt(size), orderBy, orderDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/post/status")
    public ResponseEntity<PageResponse<PostOverviewResponse>> getPostsByStatus(
            @RequestHeader("X-UserId") String userId,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size,
            @RequestParam(value = "status") String status,
            @RequestParam(value = "kw", defaultValue = "") String keyword) {
        PageResponse<PostOverviewResponse> response = postService.findAllByStatus(status, keyword, userId,
                Integer.parseInt(page), Integer.parseInt(size));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/post/{postId}")
    public ResponseEntity<MessageResponse> updatePost(
            @RequestHeader("X-UserId") String userId,
            @PathVariable("postId") String postId,
            @RequestBody PostCreateRequest request) throws AccessDeniedException {
        MessageResponse response = postService.updatePost(postId, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/post/{postId}")
    public ResponseEntity<MessageResponse> deletePost(
            @RequestHeader("X-Roles") List<String> roles,
            @RequestHeader("X-UserId") String userId,
            @PathVariable("postId") String postId) {
        MessageResponse response = postService.inActivePost(userId, roles, postId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/post/{postId}/hide")
    public ResponseEntity<MessageResponse> hide(
            @RequestHeader("X-Roles") List<String> roles,
            @RequestHeader("X-UserId") String userId,
            @PathVariable("postId") String postId) {
        MessageResponse response = postService.hidePost(userId, roles, postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/history/{assetId}")
    public ResponseEntity<PageResponse<PostHistoryResponse>> getHistoryPost(
            @RequestHeader("X-UserId") String userId,
            @PathVariable("assetId") String assetId,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size) {
        PageResponse<PostHistoryResponse> response = postService.findHistoryPost(assetId, userId,
                Integer.parseInt(page), Integer.parseInt(size));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/status")
    public ResponseEntity<List<StatusCountResponse>> getSummaryPostStatus(
            @RequestHeader("X-UserId") String userId
    ) {
        List<StatusCountResponse> response = postService.countStatusPost(userId);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/post")
    public ResponseEntity<MessageResponse> createPost(
            @RequestHeader("X-UserId") String userId,
            @RequestBody PostCreateRequest request,
            HttpServletRequest httpServletReques) {
        MessageResponse response = postService.createPost(request, userId, httpServletReques);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/post/{postId}/accept")
    public ResponseEntity<MessageResponse> acceptPost(
            @RequestHeader("X-Roles") List<String> roles,
            @PathVariable("postId") String postId) {
        MessageResponse response = postService.acceptPost(roles, postId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/post/{postId}/reject")
    public ResponseEntity<MessageResponse> rejectPost(
            @RequestHeader("X-Roles") List<String> roles,
            @PathVariable("postId") String postId) {
        MessageResponse response = postService.rejectPost(roles, postId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/post/{postId}/like")
    public ResponseEntity<MessageResponse> likePost(
            @PathVariable("postId") String postId,
            @RequestParam("userId") String userId) {
        MessageResponse response = postService.likePost(userId, postId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/post/{postId}/unlike")
    public ResponseEntity<MessageResponse> unlikePost(
            @PathVariable("postId") String postId,
            @RequestParam("userId") String userId) {
        MessageResponse response = postService.unlikePost(userId, postId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("public/post/{postId}/comments")
    public ResponseEntity<PageResponse> getComments(
            @PathVariable("postId") String postId,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size) {
        PageResponse response = postService.findCommentsByPostId(postId, Integer.parseInt(page), Integer.parseInt(size));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/post/{postId}/comment")
    public ResponseEntity<MessageResponse> createComment(
            @RequestHeader("X-UserId") String userId,
            @PathVariable("postId") String postId,
            @RequestBody Map<String, String> params) {
        MessageResponse response = postService.createComment(postId, userId, params);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/post/{postId}/comment")
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable("postId") String postId,
            @RequestBody Map<String, String> params) {
        MessageResponse response = postService.deleteComment(postId, params);

        return ResponseEntity.ok(response);
    }

}
