package com.tanle.tland.post_service.repo;

import com.tanle.tland.post_service.entity.Comment;
import com.tanle.tland.post_service.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepo extends JpaRepository<Comment, String> {
    Page<Comment> findAllByPostId(String postId, Pageable pageable);
}
