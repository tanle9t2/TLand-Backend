package com.tanle.tland.payment_service.controller;

import com.tanle.tland.payment_service.response.IpnResponse;
import com.tanle.tland.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/vn-pay-callback")
    public IpnResponse payCallbackHandler(@RequestParam Map<String, String> params) {
        return paymentService.process(params);
    }
}
