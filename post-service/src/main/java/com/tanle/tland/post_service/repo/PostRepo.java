package com.tanle.tland.post_service.repo;

import com.tanle.tland.post_service.entity.Post;
import com.tanle.tland.post_service.entity.PostStatus;
import com.tanle.tland.post_service.entity.PostType;
import com.tanle.tland.post_service.projection.PostOverview;
import com.tanle.tland.post_service.projection.StatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepo extends JpaRepository<Post, String> {
    Page<Post> findAllByType(Pageable pageable, PostType type);

    @Query("""
                from Post p 
                WHERE p.title like concat('%',:kw,'%') 
                AND p.status =:status
            """)
    Page<PostOverview> findAllByStatus(Pageable pageable, @Param("kw") String kw, @Param("status") PostStatus status);

    @Query(value = """
            SELECT s.status AS status, COUNT(p.id) AS count
            FROM (
                SELECT 'SHOW' AS status
                UNION ALL SELECT 'EXPIRED'
                UNION ALL SELECT 'REJECT'
                UNION ALL SELECT 'PAYMENT'
                UNION ALL SELECT 'HIDE'
                UNION ALL SELECT 'WAITING_PAYMENT'
                UNION ALL SELECT 'WAITING_ACCEPT'
            ) AS s
            LEFT JOIN post p ON p.status = s.status
            AND p.user_id =:userId
            GROUP BY s.status
            """, nativeQuery = true)
    List<StatusCount> countStatusPost(@Param("userId") String userId);
}
