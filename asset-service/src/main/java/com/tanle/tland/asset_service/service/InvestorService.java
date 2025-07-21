package com.tanle.tland.asset_service.service;

import com.tanle.tland.asset_service.request.InvestorCreateRequest;
import com.tanle.tland.asset_service.response.MessageResponse;

public interface InvestorService {
    public MessageResponse createInvestor(InvestorCreateRequest request);
}
