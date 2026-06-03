package com.dto.common;

/**
 * Standard API health response DTO
 * Used across all external API service implementations
 */
public class ApiHealthResponse {
    private String status;
    private long timestamp;

    public ApiHealthResponse() {
    }

    public ApiHealthResponse(String status, long timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ApiHealthResponse{" +
                "status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
