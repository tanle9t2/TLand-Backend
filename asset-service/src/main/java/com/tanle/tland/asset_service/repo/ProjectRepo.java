package com.tanle.tland.asset_service.repo;

import com.tanle.tland.asset_service.entity.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepo extends MongoRepository<Project, String> {
}
