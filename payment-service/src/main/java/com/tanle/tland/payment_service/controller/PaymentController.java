package com.tanle.tland.payment_service.controller;

import com.tanle.tland.payment_service.request.PaymentUrlRequest;
import com.tanle.tland.payment_service.response.InitPaymentResponse;
import com.tanle.tland.payment_service.response.IpnResponse;
import com.tanle.tland.payment_service.service.impl.PaymentServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentServiceImpl paymentService;

    @PostMapping("/payment-url")
    public InitPaymentResponse createPaymentUrl(@RequestBody PaymentUrlRequest request, HttpServletRequest servletRequest) {
        return paymentService.getPaymentUrl(request, servletRequest);
    }

    @GetMapping("/vn-pay-callback")
    public IpnResponse payCallbackHandler(@RequestParam Map<String, String> params) {
        log.info("Receive callback request");
        return paymentService.process(params);
    }
}
