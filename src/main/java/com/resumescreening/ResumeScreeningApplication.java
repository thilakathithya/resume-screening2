package com.resumescreening;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Resume Screening Agent.
 *
 * This application acts as an AI Agent that:
 *  1. Accepts a Job Requirement (title, description, required skills, experience)
 *  2. Accepts one or more candidate resumes (PDF / DOCX / TXT)
 *  3. Extracts resume text
 *  4. Builds a carefully engineered prompt (see PromptBuilderService)
 *  5. Sends the prompt to Google Gemini (LLM API) for structured evaluation
 *  6. Parses the structured JSON response from Gemini
 *  7. Persists results in the database and exposes them via REST APIs
 *  8. Frontend (HTML/CSS/JS) displays ranked, filtered candidates
 */
@SpringBootApplication
public class ResumeScreeningApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResumeScreeningApplication.class, args);
    }
}
