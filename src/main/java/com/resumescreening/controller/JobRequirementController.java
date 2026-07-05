package com.resumescreening.controller;

import com.resumescreening.dto.JobRequirementDto;
import com.resumescreening.exception.ResourceNotFoundException;
import com.resumescreening.model.JobRequirement;
import com.resumescreening.repository.JobRequirementRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobRequirementController {

    private final JobRequirementRepository jobRepository;

    public JobRequirementController(JobRequirementRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @PostMapping
    public ResponseEntity<JobRequirement> createJob(@Valid @RequestBody JobRequirementDto dto) {
        JobRequirement job = new JobRequirement();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setRequiredSkills(dto.getRequiredSkills());
        job.setNiceToHaveSkills(dto.getNiceToHaveSkills());
        job.setMinExperienceYears(dto.getMinExperienceYears());
        job.setQualification(dto.getQualification());
        return ResponseEntity.status(HttpStatus.CREATED).body(jobRepository.save(job));
    }

    @GetMapping
    public List<JobRequirement> listJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public JobRequirement getJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job requirement not found with id: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
