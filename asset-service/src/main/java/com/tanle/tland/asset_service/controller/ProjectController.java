package com.tanle.tland.asset_service.controller;

import com.tanle.tland.asset_service.request.ProjectCreateRequest;
import com.tanle.tland.asset_service.response.MessageResponse;
import com.tanle.tland.asset_service.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping("/project")
    public ResponseEntity<MessageResponse> createProject(@RequestBody ProjectCreateRequest request) {
        MessageResponse response = projectService.createProject(request);

        return ResponseEntity.ok(response);
    }
}
