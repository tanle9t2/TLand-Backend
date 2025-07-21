package com.tanle.tland.asset_service.service;

import com.tanle.tland.asset_service.request.ProjectCreateRequest;
import com.tanle.tland.asset_service.response.MessageResponse;

public interface ProjectService {
    MessageResponse createProject(ProjectCreateRequest request);
}
