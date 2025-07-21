package com.tanle.tland.post_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "post")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PostStatus status;


    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostLike> userLike;
}
