package com.controller.resource_controllers;

import com.dto.ApiResponse;
import com.entity.resource_entities.Resource;
import com.service_interface.resource_service_interface.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resource")
@CrossOrigin
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN','HR-MANAGER')")
    public ResponseEntity<ApiResponse> createResource(@RequestBody Resource resource) {
        return resourceService.createResource(resource);
    }

    @GetMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN','HR-MANAGER','PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse> getResourceById(@PathVariable Long resourceId) {
        return resourceService.getResourceById(resourceId);
    }

    @GetMapping("/employee-code/{employeeCode}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN','HR-MANAGER','PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse> getResourceByEmployeeCode(@PathVariable String employeeCode) {
        return resourceService.getResourceByEmployeeCode(employeeCode);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN','HR-MANAGER')")
    public ResponseEntity<ApiResponse> updateResource(@RequestBody Resource resource) {
        return resourceService.updateResource(resource);
    }
}
