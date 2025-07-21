package com.tanle.tland.asset_service.repo;

import com.tanle.tland.asset_service.entity.Investor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorRepo extends MongoRepository<Investor, String> {
}
