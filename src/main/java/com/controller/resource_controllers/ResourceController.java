package com.controller.resource_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.entity.resource_entities.Resource;
import com.service_interface.resource_service_interface.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('Resource_Manager','Admin')")
    public ResponseEntity<ApiResponse<?>> createResource(@RequestBody Resource resource) {
        return resourceService.createResource(resource);
    }

    @GetMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('Resource_Manager','Admin','Project_Manager')")
    public ResponseEntity<ApiResponse<?>> getResourceById(@PathVariable String resourceId) {
        return resourceService.getResourceById(resourceId);
    }

    @GetMapping("/employee-code/{employeeCode}")
    @PreAuthorize("hasAnyRole('Resource_Manager','Admin','Project_Manager')")
    public ResponseEntity<ApiResponse<?>> getResourceByEmployeeCode(@PathVariable String employeeCode) {
        return resourceService.getResourceByEmployeeCode(employeeCode);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('Resource_Manager','Admin')")
    public ResponseEntity<ApiResponse<?>> updateResource(@RequestBody Resource resource) {
        return resourceService.updateResource(resource);
    }

    @DeleteMapping("/delete/{resourceId}")
    @PreAuthorize("hasAnyRole('Resource_Manager','Admin')")
    public ResponseEntity<ApiResponse<?>> deleteResource(@PathVariable String resourceId) {
        return resourceService.deleteResource(resourceId);
    }

    @GetMapping("/get-all-resource-filters")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<?>> getAllResourcesFilters() {
        return resourceService.getAllResources();
    }

    @GetMapping("/get-all-resources")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<?>> getAllResources() {
        return resourceService.getResources();
    }
}
