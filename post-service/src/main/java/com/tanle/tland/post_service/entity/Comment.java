package com.tanle.tland.post_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String userId;
    private String content;
    @CreatedDate
    private LocalDateTime createdAt;
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
}
