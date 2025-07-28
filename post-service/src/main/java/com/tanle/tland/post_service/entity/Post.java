package com.tanle.tland.post_service.entity;

import com.tanle.tland.post_service.exception.UnauthorizedException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "post")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@EntityListeners(AuditingEntityListener.class)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "createdAt")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name = "user_id")
    private String userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private PostType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PostStatus status;


    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> userLike;

    public void addComment(Comment comment) {
        if (comments == null)
            comments = new ArrayList<>();

        comment.setPost(this);
        comments.add(comment);
    }

    public void removeComment(Comment comment) {
        if (comments == null)
            comments = new ArrayList<>();

        comments.remove(comment);
        comment.setPost(null);
    }

    public void addLikePost(PostLike postLike) {
        if (userLike == null)
            userLike = new ArrayList<>();

        postLike.setPost(this);
        userLike.add(postLike);
    }

    public void removeLikePost(String userId) {
        if (userLike == null)
            userLike = new ArrayList<>();

        List<PostLike> postLike = userLike.stream()
                .filter(p -> p.getUserId().equals(userId))
                .collect(Collectors.toList());
        if (postLike.isEmpty())
            throw new RuntimeException("Something wrong!!");

        userLike.remove(postLike.get(0));
        postLike.get(0).setPost(null);
    }
}
