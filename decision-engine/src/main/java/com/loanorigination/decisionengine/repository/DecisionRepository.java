package com.loanorigination.decisionengine.repository;

import com.loanorigination.decisionengine.entity.Decision;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long> {
    Optional<Decision> findByRequestId(String requestId);
    List<Decision> findAllByOrderByTimestampDesc(Pageable pageable);
}

