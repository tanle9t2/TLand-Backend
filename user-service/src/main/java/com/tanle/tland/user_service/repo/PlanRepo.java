package com.tanle.tland.user_service.repo;

import com.tanle.tland.user_service.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface PlanRepo extends JpaRepository<Plan, String> {
}
