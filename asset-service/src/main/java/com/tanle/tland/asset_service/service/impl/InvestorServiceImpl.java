package com.tanle.tland.asset_service.service.impl;

import com.tanle.tland.asset_service.entity.Investor;
import com.tanle.tland.asset_service.mapper.InvestorMapper;
import com.tanle.tland.asset_service.repo.InvestorRepo;
import com.tanle.tland.asset_service.request.InvestorCreateRequest;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.InvestorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvestorServiceImpl implements InvestorService {
    private final InvestorMapper investorMapper;
    private final InvestorRepo investorRepo;

    @Override
    public MessageResponse createInvestor(InvestorCreateRequest request) {
        Investor investor = investorMapper.convertToEntity(request);
        investor.setId(UUID.randomUUID().toString());

        investorRepo.save(investor);

        return MessageResponse.builder()
                .message("Successfully create investor")
                .status(HttpStatus.CREATED)
                .build();
    }
}
