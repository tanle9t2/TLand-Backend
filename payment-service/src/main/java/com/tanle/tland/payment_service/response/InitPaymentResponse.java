package com.tanle.tland.payment_service.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InitPaymentResponse {
    private String vnpUrl;
}
