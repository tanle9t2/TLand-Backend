package com.tanle.tland.payment_service.request;

import com.tanle.tland.payment_service.entity.PurposeType;
import com.tanle.tland.payment_service.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String userId;
    private String purposeId;
    private Double amount;
    private PurposeType purposeType;
    private TransactionType transactionType;
}
