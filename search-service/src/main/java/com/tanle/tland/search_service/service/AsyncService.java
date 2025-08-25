package com.tanle.tland.search_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public interface AsyncService {
    void migrateData();
    void createPost(String postId);

    void updatePost(String postId);

    void deletePost(String postId);
}
