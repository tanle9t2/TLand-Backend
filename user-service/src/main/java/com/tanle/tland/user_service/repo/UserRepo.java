package com.tanle.tland.user_service.repo;

import com.tanle.tland.user_service.entity.User;
import com.tanle.tland.user_service.projection.UserProfile;
import jakarta.ws.rs.DELETE;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    @Query("FROM User u where u.id =:userId")
    Optional<UserProfile> findProfileUser(@Param("userId") String userId);

    <T> Optional<T> findById(String id, Class<T> type);

    @Query(value = """
                SELECT COUNT(*) FROM  Follower as f
                WHERE f.user_id =:userId
            """, nativeQuery = true)
    Long countFollower(@Param("userId") String userId);

    @Query(value = """
                SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
                FROM follower
                WHERE follower_id =:userId
                  AND user_id =:followerId
            """, nativeQuery = true)
    Long checkFollow(@Param("userId") String userId, @Param("followerId") String followerId);

    @Query(value = """
                SELECT COUNT(*) FROM  Follower as f
                WHERE f.follower_id =:userId
            """, nativeQuery = true)
    Long countFlowing(@Param("userId") String userId);

    @Query(value = """
            SELECT u.* FROM User u 
            JOIN follower f
            where u.user_id = f.follower_id
            AND f.follower_id =:userId
            """, nativeQuery = true)
    Page<User> findFollowerByUserId(@Param("userId") String userId, Pageable pageable);

    @Query(value = """
            SELECT u.* FROM User u 
            JOIN follower f
            where u.user_id = f.user_id
            AND f.user_id =:userId
            """, nativeQuery = true)
    Page<User> findFollowingByUserId(@Param("userId") String userId, Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO follower (user_id, follower_id) 
            VALUES (:userId, :followerId)
            """, nativeQuery = true)
    void followUser(@Param("userId") String userId, @Param("followerId") String followerId);

    @Modifying
    @Query(value = """
            DELETE FROM follower f
            WHERE f.user_id =:userId
            AND f.follower_id=:followerId
            """, nativeQuery = true)
    void unfollowUser(@Param("userId") String userId, @Param("followerId") String followerId);
}
