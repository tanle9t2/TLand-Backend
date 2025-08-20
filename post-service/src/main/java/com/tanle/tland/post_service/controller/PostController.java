package com.tanle.tland.post_service.controller;

import com.google.api.Page;
import com.tanle.tland.post_service.entity.PostStatus;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.*;
import com.tanle.tland.post_service.service.PostService;
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
    public ResponseEntity<PostResponse> getPostByID(@PathVariable("postId") String postId) {
        PostResponse response = postService.findPostById(postId);
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

    @GetMapping("/public/post/status")
    public ResponseEntity<PageResponse<PostOverviewResponse>> getPostsByStatus(
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size,
            @RequestParam(value = "status") String status,
            @RequestParam(value = "kw", defaultValue = "") String keyword) {
        String userId = "eadd6456-a5ea-4d41-b71a-061541227b8d";
        PageResponse<PostOverviewResponse> response = postService.findAllByStatus(status, keyword, userId,
                Integer.parseInt(page), Integer.parseInt(size));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/post/{postId}")
    public ResponseEntity<MessageResponse> updatePost(@PathVariable("postId") String postId,
                                                      @RequestBody PostCreateRequest request) throws AccessDeniedException {
        String userId = "eadd6456-a5ea-4d41-b71a-061541227b8d";
        MessageResponse response = postService.updatePost(postId, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/post/{postId}")
    public ResponseEntity<MessageResponse> deletePost(@PathVariable("postId") String postId) {
        String userId = "eadd6456-a5ea-4d41-b71a-061541227b8d";
        MessageResponse response = postService.inActivePost(userId, postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/posts/history/{assetId}")
    public ResponseEntity<PageResponse<PostHistoryResponse>> getHistoryPost(
            @PathVariable("assetId") String assetId,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size) {
        String userId = "eadd6456-a5ea-4d41-b71a-061541227b8d";
        PageResponse<PostHistoryResponse> response = postService.findHistoryPost(assetId, userId,
                Integer.parseInt(page), Integer.parseInt(size));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/posts/status")
    public ResponseEntity<List<StatusCountResponse>> getSummaryPostStatus() {
        String userId = "eadd6456-a5ea-4d41-b71a-061541227b8d";
        List<StatusCountResponse> response = postService.countStatusPost(userId);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/post")
    public ResponseEntity<MessageResponse> createPost(@RequestBody PostCreateRequest request) {
        MessageResponse response = postService.createPost(request);

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

    @GetMapping("/post/{postId}/comments")
    public ResponseEntity<PageResponse> getComments(
            @PathVariable("postId") String postId,
            @RequestParam(value = "page", defaultValue = PAGE_DEFAULT) String page,
            @RequestParam(value = "size", defaultValue = PAGE_SIZE) String size) {
        PageResponse response = postService.findCommentsByPostId(postId, Integer.parseInt(page), Integer.parseInt(size));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/post/{postId}/comment")
    public ResponseEntity<MessageResponse> createComment(
            @PathVariable("postId") String postId,
            @RequestBody Map<String, String> params) {
        MessageResponse response = postService.createComment(postId, params);

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
