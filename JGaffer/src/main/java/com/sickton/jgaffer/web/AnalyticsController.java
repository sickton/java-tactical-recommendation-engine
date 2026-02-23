package com.sickton.jgaffer.web;

import com.sickton.jgaffer.persistence.dto.TacticAnalytics;
import com.sickton.jgaffer.service.analytics.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/win-rate-by-tactic")
    public List<TacticAnalytics> getWinRateByTactic() {
        return analyticsService.getWinRateByTactic();
    }
}