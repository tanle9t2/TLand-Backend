package com.tanle.tland.post_service.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PaymentRequest {
    private String purposeId;
    private String purposeType;
    private Integer userId;
    private Double amount;
}
