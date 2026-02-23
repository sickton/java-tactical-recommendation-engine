package com.sickton.jgaffer.service.analytics;

import com.sickton.jgaffer.persistence.dto.TacticAnalytics;
import com.sickton.jgaffer.persistence.repository.MatchDecisionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService{
    private final MatchDecisionRepository repository;

    public AnalyticsService(MatchDecisionRepository repository) {
        this.repository = repository;
    }

    public List<TacticAnalytics> getWinRateByTactic(String league) {
        return repository.getWinRateByTactic(league)
                .stream()
                .map(stat -> {
                    double winRate = 0.0;
                    if (stat.getTotal() > 0) {
                        winRate = (stat.getWins() * 100.0) / stat.getTotal();
                    }

                    return new TacticAnalytics(
                            stat.getStartTactic(),
                            stat.getTotal(),
                            stat.getWins(),
                            Math.round(winRate * 100.0) / 100.0
                    );
                })
                .toList();
    }
}
