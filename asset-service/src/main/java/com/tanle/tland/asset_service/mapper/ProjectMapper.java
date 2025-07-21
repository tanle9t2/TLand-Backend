package com.tanle.tland.asset_service.mapper;

import com.tanle.tland.asset_service.entity.Project;
import com.tanle.tland.asset_service.request.ProjectCreateRequest;
import com.tanle.tland.asset_service.response.ProjectResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {AssetMapper.class})
public interface ProjectMapper {

    Project convertToEntity(ProjectCreateRequest request);

    ProjectResponse convertToResponse(Project project);
}
