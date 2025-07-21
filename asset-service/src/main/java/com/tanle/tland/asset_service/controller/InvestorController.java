package com.tanle.tland.asset_service.controller;

import com.tanle.tland.asset_service.entity.Investor;
import com.tanle.tland.asset_service.request.InvestorCreateRequest;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.InvestorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class InvestorController {
    private final InvestorService investorService;

    @PostMapping("/investor")
    public ResponseEntity<MessageResponse> createInvestor(@RequestBody InvestorCreateRequest request) {
        MessageResponse response = investorService.createInvestor(request);

        return ResponseEntity.ok(response);
    }
}
