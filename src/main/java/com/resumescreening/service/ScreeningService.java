package com.resumescreening.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.dto.ScreeningResultDto;
import com.resumescreening.exception.LlmProcessingException;
import com.resumescreening.exception.ResourceNotFoundException;
import com.resumescreening.model.CandidateResume;
import com.resumescreening.model.JobRequirement;
import com.resumescreening.model.ScreeningResult;
import com.resumescreening.repository.CandidateResumeRepository;
import com.resumescreening.repository.JobRequirementRepository;
import com.resumescreening.repository.ScreeningResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScreeningService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningService.class);

    private final JobRequirementRepository jobRepository;
    private final CandidateResumeRepository resumeRepository;
    private final ScreeningResultRepository resultRepository;
    private final ResumeTextExtractorService extractorService;
    private final PromptBuilderService promptBuilderService;
    private final GeminiClientService geminiClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScreeningService(JobRequirementRepository jobRepository,
                             CandidateResumeRepository resumeRepository,
                             ScreeningResultRepository resultRepository,
                             ResumeTextExtractorService extractorService,
                             PromptBuilderService promptBuilderService,
                             GeminiClientService geminiClientService) {
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.resultRepository = resultRepository;
        this.extractorService = extractorService;
        this.promptBuilderService = promptBuilderService;
        this.geminiClientService = geminiClientService;
    }

    /**
     * Full pipeline for ONE uploaded resume against ONE job:
     * extract text -> build prompt -> call Gemini -> parse -> persist.
     */
    public ScreeningResult screenResume(Long jobId, MultipartFile file) {
        JobRequirement job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job requirement not found with id: " + jobId));

        String resumeText;
        try {
            resumeText = extractorService.extractText(file);
        } catch (Exception e) {
            throw new LlmProcessingException("Could not read resume file: " + file.getOriginalFilename(), e);
        }

        if (resumeText == null || resumeText.isBlank()) {
            throw new LlmProcessingException("Resume file appears empty or unreadable: " + file.getOriginalFilename());
        }

        // Persist raw resume record first
        CandidateResume resume = new CandidateResume();
        resume.setFileName(file.getOriginalFilename());
        resume.setResumeText(resumeText);
        resume.setJob(job);
        resume.setCandidateName(extractorService.guessNameFromFileName(file.getOriginalFilename()));
        resume = resumeRepository.save(resume);

        // Prompt engineering step
        String prompt = promptBuilderService.buildScreeningPrompt(job, resumeText);

        // Call Gemini LLM
        String llmRawResponse = geminiClientService.generateContent(prompt);

        // Parse structured JSON response
        ScreeningResultDto dto = parseLlmResponse(llmRawResponse);

        // Persist screening result
        ScreeningResult result = new ScreeningResult();
        result.setJob(job);
        result.setResume(resume);
        result.setMatchScore(clampScore(dto.getMatchScore()));
        result.setVerdict(dto.getVerdict() != null ? dto.getVerdict() : "NOT_RECOMMENDED");
        result.setMatchedSkills(joinArray(dto.getMatchedSkills()));
        result.setMissingSkills(joinArray(dto.getMissingSkills()));
        result.setSummary(dto.getSummary());
        result.setCandidateExperienceYears(dto.getCandidateExperienceYears());
        result.setRawLlmResponse(llmRawResponse);

        // Update candidate name on the resume if the LLM extracted one
        if (dto.getCandidateName() != null && !dto.getCandidateName().isBlank()) {
            resume.setCandidateName(dto.getCandidateName());
            resumeRepository.save(resume);
        }

        return resultRepository.save(result);
    }

    public List<ScreeningResult> getRankedResults(Long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job requirement not found with id: " + jobId);
        }
        return resultRepository.findByJobIdOrderByMatchScoreDesc(jobId);
    }

    private ScreeningResultDto parseLlmResponse(String rawResponse) {
        String cleaned = stripCodeFences(rawResponse);
        try {
            return objectMapper.readValue(cleaned, ScreeningResultDto.class);
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response: {}", rawResponse, e);
            throw new LlmProcessingException("LLM returned a response that could not be parsed as JSON.", e);
        }
    }

    /** Defensive cleanup in case the model wraps JSON in ```json ... ``` fences despite instructions. */
    private String stripCodeFences(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```[a-zA-Z]*", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private Integer clampScore(Integer score) {
        if (score == null) return 0;
        return Math.max(0, Math.min(100, score));
    }

    private String joinArray(String[] arr) {
        if (arr == null) return "";
        return String.join(", ", List.of(arr).stream().map(String::trim).collect(Collectors.toList()));
    }
}
