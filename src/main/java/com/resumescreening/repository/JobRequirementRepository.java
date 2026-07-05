package com.resumescreening.repository;

import com.resumescreening.model.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRequirementRepository extends JpaRepository<JobRequirement, Long> {
}
