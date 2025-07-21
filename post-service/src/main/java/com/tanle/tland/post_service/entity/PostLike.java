package com.tanle.tland.post_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_like")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PostLike {

    @Id
    private String id;

    @ManyToOne
    @MapsId
    @JoinColumn(name = "id") // 👈 maps to user.id
    private Post post;

    private String userId;
}
