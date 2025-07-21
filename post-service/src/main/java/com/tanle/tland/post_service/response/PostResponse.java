package com.tanle.tland.post_service.response;

import com.tanle.tland.post_service.entity.Comment;
import com.tanle.tland.post_service.entity.PostLike;
import com.tanle.tland.post_service.entity.PostStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PostResponse {
    private String id;
    private String title;
    private String description;
    private LocalDateTime lastUpdated;

    private String userId;
    private String status;


}
