package com.resumescreening.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "screening_results")
public class ScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    @JsonIgnore
    private JobRequirement job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private CandidateResume resume;

    /** 0 - 100 match score computed by the LLM */
    private Integer matchScore;

    /** STRONGLY_RECOMMENDED, RECOMMENDED, NOT_RECOMMENDED */
    private String verdict;

    @Column(length = 1000)
    private String matchedSkills;

    @Column(length = 1000)
    private String missingSkills;

    @Column(length = 2000)
    private String summary;

    private Integer candidateExperienceYears;

    @Lob
    @Column(columnDefinition = "CLOB")
    @JsonIgnore
    private String rawLlmResponse;

    private LocalDateTime screenedAt = LocalDateTime.now();

    public ScreeningResult() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JobRequirement getJob() { return job; }
    public void setJob(JobRequirement job) { this.job = job; }

    public CandidateResume getResume() { return resume; }
    public void setResume(CandidateResume resume) { this.resume = resume; }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(String matchedSkills) { this.matchedSkills = matchedSkills; }

    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Integer getCandidateExperienceYears() { return candidateExperienceYears; }
    public void setCandidateExperienceYears(Integer candidateExperienceYears) { this.candidateExperienceYears = candidateExperienceYears; }

    public String getRawLlmResponse() { return rawLlmResponse; }
    public void setRawLlmResponse(String rawLlmResponse) { this.rawLlmResponse = rawLlmResponse; }

    public LocalDateTime getScreenedAt() { return screenedAt; }
    public void setScreenedAt(LocalDateTime screenedAt) { this.screenedAt = screenedAt; }
}
