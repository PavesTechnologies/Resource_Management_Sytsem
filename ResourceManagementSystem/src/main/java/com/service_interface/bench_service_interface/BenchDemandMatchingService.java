package com.service_interface.bench_service_interface;

import com.dto.bench_dto.MatchResponse;

import java.util.List;

public interface BenchDemandMatchingService {
    List<MatchResponse> getMatches();
    List<MatchResponse> getMatches(String skill, Integer minExp);
}
