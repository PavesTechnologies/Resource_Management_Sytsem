package com.service_interface.timeline_interface;

import com.dto.ResourceTimelineDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ResourceTimelineService {
    
    List<ResourceTimelineDTO> getAllResourceTimelines();
}
