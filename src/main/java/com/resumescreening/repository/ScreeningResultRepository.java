package com.resumescreening.repository;

import com.resumescreening.model.ScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, Long> {

    List<ScreeningResult> findByJobIdOrderByMatchScoreDesc(Long jobId);

    List<ScreeningResult> findByResumeId(Long resumeId);
}
