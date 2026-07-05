package com.resumescreening.controller;

import com.resumescreening.model.ScreeningResult;
import com.resumescreening.service.ScreeningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/screening")
public class ResumeScreeningController {

    private final ScreeningService screeningService;

    public ResumeScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    /**
     * Upload one or more resumes and screen them against the given job.
     * multipart/form-data with field name "files" (can repeat for multiple files).
     */
    @PostMapping(value = "/jobs/{jobId}/upload", consumes = "multipart/form-data")
    public ResponseEntity<List<ScreeningResult>> uploadAndScreen(
            @PathVariable Long jobId,
            @RequestParam("files") MultipartFile[] files) {

        List<ScreeningResult> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                results.add(screeningService.screenResume(jobId, file));
            } catch (Exception e) {
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        if (results.isEmpty() && !errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(results);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /** Ranked list of candidates for a job, best match first. */
    @GetMapping("/jobs/{jobId}/results")
    public List<ScreeningResult> getResults(@PathVariable Long jobId) {
        return screeningService.getRankedResults(jobId);
    }
}
