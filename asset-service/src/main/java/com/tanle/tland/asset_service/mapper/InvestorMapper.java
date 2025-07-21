package com.tanle.tland.asset_service.mapper;

import com.tanle.tland.asset_service.entity.Investor;
import com.tanle.tland.asset_service.request.InvestorCreateRequest;
import org.mapstruct.Mapper;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Mapper(componentModel = "spring")
public interface InvestorMapper {
    Investor convertToEntity(InvestorCreateRequest request);
}
