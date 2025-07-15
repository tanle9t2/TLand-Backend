package com.tanle.tland.user_service.repo;

import com.tanle.tland.user_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

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
