package com.sickton.jgaffer.persistence.repository;

import com.sickton.jgaffer.persistence.entity.MatchDecision;
import com.sickton.jgaffer.persistence.entity.TacticWinStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MatchDecisionRepository extends JpaRepository<MatchDecision, UUID> {
    @Query("""
SELECT 
    m.startTactic as startTactic,
    COUNT(m) as total,
    SUM(CASE WHEN m.outcome = 'WIN' THEN 1 ELSE 0 END) as wins
FROM MatchDecision m
WHERE m.league = :league
GROUP BY m.startTactic
""")
    List<TacticWinStats> getWinRateByTactic(@Param("league") String league);
}
