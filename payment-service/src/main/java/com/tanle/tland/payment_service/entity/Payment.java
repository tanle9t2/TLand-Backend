package com.tanle.tland.payment_service.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "amount")
    private double amount;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "purpose_type")
    @Enumerated(EnumType.STRING)
    private PaymentStatus purposeType;

    @Column(name = "transaction_type")
    @Enumerated(EnumType.STRING)
    private PaymentStatus transactionType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Column(name = "user_id")
    private LocalDateTime userId;
    @Column(name = "purpose_id")
    private LocalDateTime purposeId;

}


























