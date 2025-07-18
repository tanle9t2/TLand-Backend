package com.tanle.tland.asset_service.repo;

import com.tanle.tland.asset_service.entity.Asset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepo extends MongoRepository<Asset, String> {
}
