package com.tanle.tland.post_service.repo;

import com.tanle.tland.post_service.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepo extends JpaRepository<Post,String> {
}
