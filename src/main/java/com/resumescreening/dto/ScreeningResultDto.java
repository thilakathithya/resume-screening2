package com.resumescreening.dto;

/**
 * This is the EXACT JSON shape we instruct Gemini to return.
 * Keeping this DTO in sync with the prompt's output schema is what makes
 * parsing the LLM response reliable (a core prompt-engineering practice:
 * "constrain the output format").
 */
public class ScreeningResultDto {

    private Integer matchScore;          // 0-100
    private String verdict;              // STRONGLY_RECOMMENDED | RECOMMENDED | NOT_RECOMMENDED
    private String candidateName;
    private Integer candidateExperienceYears;
    private String[] matchedSkills;
    private String[] missingSkills;
    private String summary;

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public Integer getCandidateExperienceYears() { return candidateExperienceYears; }
    public void setCandidateExperienceYears(Integer candidateExperienceYears) { this.candidateExperienceYears = candidateExperienceYears; }

    public String[] getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(String[] matchedSkills) { this.matchedSkills = matchedSkills; }

    public String[] getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String[] missingSkills) { this.missingSkills = missingSkills; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
