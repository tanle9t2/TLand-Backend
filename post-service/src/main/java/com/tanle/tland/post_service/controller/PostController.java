package com.tanle.tland.post_service.controller;

import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.MessageResponse;
import com.tanle.tland.post_service.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;


    @PostMapping("/post")
    public ResponseEntity<MessageResponse> createPost(@RequestBody PostCreateRequest request) {
        MessageResponse response = postService.createPost(request);

        return ResponseEntity.ok(response);
    }
}
