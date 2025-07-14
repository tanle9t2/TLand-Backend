package com.tanle.tland.user_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plan_id")
    private String id;

    @Column(name = "monthly_price")
    private double monthlyPrice;
    @Column(name = "max_post")
    private int maxPost;
    @Column(name = "show_post")
    private int showPost;
}
