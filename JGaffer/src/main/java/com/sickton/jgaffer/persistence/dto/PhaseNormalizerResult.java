package com.sickton.jgaffer.persistence.dto;

import java.util.List;

public record PhaseNormalizerResult(
        String league,
        String tactic,
        List<PhasePerformance> phases
) {}