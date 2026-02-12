package com.service_imple.availability_service_impl.projection;

public interface CurrentAllocationProjection {
    Long getResourceId();
    Integer getCurrentAllocation();
}
