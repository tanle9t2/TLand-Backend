package com.tanle.tland.payment_service.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String purposeId;
    private Integer userId;
    private Double amount;


}
