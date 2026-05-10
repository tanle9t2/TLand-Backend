package com.tanle.tland.payment_service.service;


import com.tanle.tland.payment_service.request.PaymentUrlRequest;
import com.tanle.tland.payment_service.response.InitPaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {
    InitPaymentResponse getPaymentUrl(PaymentUrlRequest request, HttpServletRequest servletRequest);
}
