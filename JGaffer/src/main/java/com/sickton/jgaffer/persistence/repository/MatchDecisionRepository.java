package com.sickton.jgaffer.persistence.repository;

import com.sickton.jgaffer.persistence.entity.MatchDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchDecisionRepository extends JpaRepository<MatchDecision, UUID> {
}
