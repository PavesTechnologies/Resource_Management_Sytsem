package com.dto.allocation_dto;

import java.time.LocalDate;

public class CloseAllocationDTO {
    private LocalDate closureDate;
    private String reason;

    public LocalDate getClosureDate() {
        return closureDate;
    }

    public void setClosureDate(LocalDate closureDate) {
        this.closureDate = closureDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
