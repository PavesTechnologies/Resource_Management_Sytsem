package com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> records;
    private int page;
    private int size;
    private long totalRecords;
    private int totalPages;
}

