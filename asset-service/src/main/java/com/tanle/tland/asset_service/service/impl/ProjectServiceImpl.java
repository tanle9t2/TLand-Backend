package com.tanle.tland.asset_service.service.impl;

import com.tanle.tland.asset_service.entity.Investor;
import com.tanle.tland.asset_service.entity.Project;
import com.tanle.tland.asset_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.asset_service.mapper.ProjectMapper;
import com.tanle.tland.asset_service.repo.InvestorRepo;
import com.tanle.tland.asset_service.repo.ProjectRepo;
import com.tanle.tland.asset_service.request.ProjectCreateRequest;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepo projectRepo;
    private final InvestorRepo investorRepo;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional
    public MessageResponse createProject(ProjectCreateRequest request) {
        Investor investor = investorRepo.findById(request.getInvestorId())
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found investor: " + request.getInvestorId()));

        Project project = projectMapper.convertToEntity(request);
        project.setId(UUID.randomUUID().toString());
        projectRepo.save(project);

        return MessageResponse.builder()
                .message("Successfully create project")
                .status(HttpStatus.CREATED)
                .data(request)
                .build();
    }
}
