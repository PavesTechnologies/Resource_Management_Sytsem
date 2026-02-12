package com.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceTimelineApiResponse {
    private Boolean success;
    private String message;
    private List<ResourceTimelineResponseDTO> data;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;

    public static ResourceTimelineApiResponse success(String message, List<ResourceTimelineResponseDTO> data, int page, int size, long totalCount) {
        return ResourceTimelineApiResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .page(page)
                .size(size)
                .totalElements(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / size))
                .build();
    }

    public static ResourceTimelineApiResponse error(String message) {
        return ResourceTimelineApiResponse.builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
