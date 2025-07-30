package com.tanle.tland.post_service.repo;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.entity.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepo extends JpaRepository<Post, String> {
    Page<Post> findAllByType(Pageable pageable, PostType type);
}
