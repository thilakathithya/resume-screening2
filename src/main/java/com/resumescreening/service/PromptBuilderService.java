package com.resumescreening.service;

import com.resumescreening.model.JobRequirement;
import org.springframework.stereotype.Service;

/**
 * Builds the prompt sent to the Gemini LLM.
 *
 * PROMPT ENGINEERING PRINCIPLES APPLIED HERE:
 *  1. ROLE PROMPTING       - The model is told exactly who it is ("expert technical recruiter").
 *  2. CLEAR TASK           - The task is spelled out step by step, no ambiguity.
 *  3. DELIMITERS           - Job description and resume text are wrapped in XML-like tags
 *                            so the model never confuses instructions with data.
 *  4. STRUCTURED OUTPUT    - The model is forced to return ONLY JSON matching a strict schema,
 *                            which makes downstream parsing deterministic and reliable.
 *  5. FEW-SHOT EXAMPLE     - One example input/output pair anchors the exact format expected.
 *  6. CHAIN-OF-THOUGHT     - The model is told to privately reason step-by-step, but to expose
 *                            only the final JSON (keeps output clean while improving quality).
 *  7. GROUNDING / GUARDRAILS - Explicit instruction not to hallucinate skills/experience that
 *                            are not present in the resume text.
 */
@Service
public class PromptBuilderService {

    public String buildScreeningPrompt(JobRequirement job, String resumeText) {
        StringBuilder prompt = new StringBuilder();

        // 1. ROLE PROMPTING
        prompt.append("You are an expert technical recruiter and resume screening AI agent ")
              .append("with 15 years of experience hiring for software and technology roles. ")
              .append("You are precise, unbiased, and evidence-based in your evaluations.\n\n");

        // 2. CLEAR TASK DEFINITION
        prompt.append("TASK:\n")
              .append("Compare the CANDIDATE RESUME against the JOB REQUIREMENT below and produce ")
              .append("a structured evaluation of how well the candidate fits the role.\n\n");

        // 3. DELIMITED CONTEXT - Job Requirement
        prompt.append("<job_requirement>\n")
              .append("Title: ").append(safe(job.getTitle())).append("\n")
              .append("Description: ").append(safe(job.getDescription())).append("\n")
              .append("Required Skills: ").append(safe(job.getRequiredSkills())).append("\n")
              .append("Nice-to-have Skills: ").append(safe(job.getNiceToHaveSkills())).append("\n")
              .append("Minimum Experience (years): ").append(job.getMinExperienceYears() == null ? "Not specified" : job.getMinExperienceYears()).append("\n")
              .append("Qualification: ").append(safe(job.getQualification())).append("\n")
              .append("</job_requirement>\n\n");

        // 3. DELIMITED CONTEXT - Resume
        prompt.append("<candidate_resume>\n")
              .append(safe(resumeText))
              .append("\n</candidate_resume>\n\n");

        // 6. CHAIN OF THOUGHT (internal only)
        prompt.append("INSTRUCTIONS:\n")
              .append("1. Silently think step by step: extract the candidate's name, total years of experience, ")
              .append("and skills actually mentioned in the resume text.\n")
              .append("2. Compare each required skill against the resume. Only count a skill as matched if it is ")
              .append("explicitly evidenced in the resume text — never assume or invent skills.\n")
              .append("3. Compute a matchScore from 0 to 100 based on: required skill coverage (60% weight), ")
              .append("relevant experience vs minimum required (25% weight), and nice-to-have skills / qualification (15% weight).\n")
              .append("4. Decide a verdict:\n")
              .append("   - STRONGLY_RECOMMENDED  -> matchScore >= 80\n")
              .append("   - RECOMMENDED           -> matchScore between 50 and 79\n")
              .append("   - NOT_RECOMMENDED       -> matchScore < 50\n")
              .append("5. Write a concise 2-3 sentence summary explaining the verdict, mentioning strongest and weakest points.\n")
              .append("6. Do NOT reveal your step-by-step reasoning in the output. Output ONLY the final JSON object.\n\n");

        // 4. STRICT OUTPUT SCHEMA
        prompt.append("OUTPUT FORMAT:\n")
              .append("Return ONLY a valid JSON object (no markdown fences, no commentary, no preamble) ")
              .append("with EXACTLY this schema:\n")
              .append("{\n")
              .append("  \"candidateName\": string,\n")
              .append("  \"candidateExperienceYears\": number,\n")
              .append("  \"matchScore\": number,\n")
              .append("  \"verdict\": \"STRONGLY_RECOMMENDED\" | \"RECOMMENDED\" | \"NOT_RECOMMENDED\",\n")
              .append("  \"matchedSkills\": [string],\n")
              .append("  \"missingSkills\": [string],\n")
              .append("  \"summary\": string\n")
              .append("}\n\n");

        // 5. FEW-SHOT EXAMPLE
        prompt.append("EXAMPLE OUTPUT (for a different, unrelated job/resume, purely to show format):\n")
              .append("{\n")
              .append("  \"candidateName\": \"Priya Sharma\",\n")
              .append("  \"candidateExperienceYears\": 3,\n")
              .append("  \"matchScore\": 72,\n")
              .append("  \"verdict\": \"RECOMMENDED\",\n")
              .append("  \"matchedSkills\": [\"Java\", \"Spring Boot\", \"REST APIs\"],\n")
              .append("  \"missingSkills\": [\"Kubernetes\", \"AWS\"],\n")
              .append("  \"summary\": \"Priya has strong core backend skills and 3 years of relevant experience, ")
              .append("covering most required skills. She lacks cloud/DevOps exposure (Kubernetes, AWS) which the role needs.\"\n")
              .append("}\n\n");

        prompt.append("Now produce the JSON output for the CANDIDATE RESUME and JOB REQUIREMENT given above. ")
              .append("Remember: JSON only, matching the schema exactly.");

        return prompt.toString();
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "Not specified" : value.trim();
    }
}
