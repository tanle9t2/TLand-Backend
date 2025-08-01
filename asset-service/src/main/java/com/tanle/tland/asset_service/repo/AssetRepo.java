package com.tanle.tland.asset_service.repo;

import com.tanle.tland.asset_service.entity.Asset;
import com.tanle.tland.asset_service.entity.AssetType;
import com.tanle.tland.asset_service.projection.AssetSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepo extends MongoRepository<Asset, String> {
    Page<AssetSummary> findAllByUserId(String userId, Pageable pageable);

    List<Asset> findAllByTypeAndUserId(AssetType type, String userId);
}
