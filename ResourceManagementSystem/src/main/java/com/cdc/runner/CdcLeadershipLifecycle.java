package com.cdc.runner;

public interface CdcLeadershipLifecycle {
    void onLeadershipAcquired();
    void onLeadershipLost();
}
