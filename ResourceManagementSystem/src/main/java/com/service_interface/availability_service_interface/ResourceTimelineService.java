package com.service_interface.availability_service_interface;

import com.dto.ResourceTimelineDTO;
import com.dto.ResourceTimelineResponseDTO;
import com.dto.ResourceTimelineApiResponse;

import java.time.LocalDate;
import java.util.List;

public interface ResourceTimelineService {
    
    List<ResourceTimelineDTO> getAllResourceTimelines();
    
    ResourceTimelineApiResponse getResourceTimelineWindow(
        LocalDate startDate, 
        LocalDate endDate,
        String designation,
        String location,
        Integer minExp,
        Integer maxExp,
        String employmentType,
        String status,
        Integer page,
        Integer size
    );
    
    ResourceTimelineResponseDTO.TimelineKPI getTimelineKPI(
        LocalDate startDate, 
        LocalDate endDate,
        String designation,
        String location,
        Integer minExp,
        Integer maxExp,
        String employmentType,
        String status
    );
}
