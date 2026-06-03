package com.dto.allocation_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeInfoDTO {
    private Boolean isNoticePeriod;
    private LocalDate noticeStartDate;
    private LocalDate noticeEndDate;
}
