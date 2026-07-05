# 🤖 Resume Screening Agent

An AI agent web application that filters and ranks candidate resumes against a job
requirement, using **Google Gemini** as the LLM brain, **Spring Boot** as the backend,
**H2** as the database, and a plain **HTML/CSS/JavaScript** frontend. Fully **Dockerized**.

---

## ✅ Requirements Coverage

| Requirement | Implementation |
|---|---|
| Prompt Engineering | `PromptBuilderService.java` — role prompting, delimiters, strict JSON output schema, few-shot example, chain-of-thought instructions, anti-hallucination guardrails |
| LLM API (Gemini) | `GeminiClientService.java` — calls Gemini `generateContent` REST API |
| Database | H2 (file-based, persisted via Docker volume) + Spring Data JPA |
| Web Framework | Spring Boot 3 (REST APIs) |
| Frontend | HTML + CSS + vanilla JavaScript (`/static`) |
| Deployment | `Dockerfile` (multi-stage build) + `docker-compose.yml` |

---

## 🏗️ Architecture

```
Browser (HTML/CSS/JS)
      │  fetch() REST calls
      ▼
Spring Boot REST Controllers
      │
      ├── JobRequirementController   -> CRUD for job postings
      └── ResumeScreeningController  -> upload resumes, get ranked results
                │
                ▼
        ScreeningService (orchestrator)
                │
      ┌─────────┼─────────────┐
      ▼         ▼             ▼
ResumeTextExtractor  PromptBuilder   GeminiClientService
(PDF/DOCX/TXT)       (prompt eng.)   (calls Gemini API)
      │                                    │
      └──────────────┬─────────────────────┘
                      ▼
            H2 Database (JobRequirement,
            CandidateResume, ScreeningResult)
```

### How the AI Agent works (pipeline)
1. HR creates a **Job Requirement** (title, description, required/nice-to-have skills, min experience).
2. HR uploads one or more **resumes** (PDF / DOCX / TXT) for that job.
3. Backend extracts raw text from each resume file (Apache PDFBox / POI).
4. `PromptBuilderService` builds a carefully engineered prompt combining the job + resume text.
5. `GeminiClientService` sends the prompt to Gemini and gets back a **strict JSON** response
   (candidate name, experience, match score, verdict, matched/missing skills, summary).
6. The result is parsed, persisted to H2, and returned to the frontend.
7. Frontend displays candidates **ranked by match score**, filterable by verdict, with a
   detail view showing the AI's reasoning summary.

---

## 🧠 Prompt Engineering Principles Applied

See `src/main/java/com/resumescreening/service/PromptBuilderService.java`. It applies:

- **Role prompting** — model is told it's an expert technical recruiter.
- **Delimiters** — job requirement and resume text are wrapped in `<job_requirement>` /
  `<candidate_resume>` tags so instructions are never confused with data.
- **Structured/constrained output** — the model must return ONLY a JSON object matching an
  exact schema (`responseMimeType: application/json` is also set on the Gemini call).
- **Few-shot example** — one example input/output pair anchors the exact format.
- **Chain-of-thought (internal)** — model is told to reason step-by-step privately, but
  expose only the final JSON, improving quality without polluting the output.
- **Anti-hallucination guardrail** — explicit instruction to only count skills that are
  evidenced in the resume text, never invented.
- **Scoring rubric** — explicit weights (60% skill coverage / 25% experience / 15% extras)
  so scores are consistent across candidates.

---

## 🚀 Running Locally (without Docker)

### Prerequisites
- Java 17+
- Maven 3.9+
- A free Gemini API key from https://aistudio.google.com/app/apikey

### Steps
```bash
export GEMINI_API_KEY=your_actual_key_here
mvn clean package -DskipTests
java -jar target/resume-screening-agent.jar
```

Open your browser at **http://localhost:8080**

The H2 database file will be created at `./data/resumedb.mv.db` in your working directory.

---

## 🐳 Running with Docker (recommended)

### 1. Set your Gemini API key
```bash
cp .env.example .env
# edit .env and paste your real GEMINI_API_KEY
```

### 2. Build & run with Docker Compose
```bash
docker compose up --build
```

Open your browser at **http://localhost:8080**

Data persists across restarts in the `resume-data` Docker volume.

### Or run with plain Docker (no compose)
```bash
docker build -t resume-screening-agent .
docker run -p 8080:8080 \
  -e GEMINI_API_KEY=your_actual_key_here \
  -v resume-data:/data \
  resume-screening-agent
```

---

## 📡 REST API Reference

### Create a job requirement
```
POST /api/jobs
Content-Type: application/json

{
  "title": "Backend Java Developer",
  "description": "Build and maintain REST APIs for our platform...",
  "requiredSkills": "Java, Spring Boot, SQL, REST APIs",
  "niceToHaveSkills": "Docker, Kubernetes, AWS",
  "minExperienceYears": 3,
  "qualification": "B.E/B.Tech in Computer Science"
}
```

### List all jobs
```
GET /api/jobs
```

### Upload & screen resumes against a job
```
POST /api/screening/jobs/{jobId}/upload
Content-Type: multipart/form-data
Field: files (one or more PDF/DOCX/TXT files)
```

### Get ranked screening results for a job
```
GET /api/screening/jobs/{jobId}/results
```
Returns candidates sorted by `matchScore` descending, each with `verdict`,
`matchedSkills`, `missingSkills`, and an AI-generated `summary`.

---

## 🗂️ Project Structure

```
resume-screening-agent/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── src/main/java/com/resumescreening/
│   ├── ResumeScreeningApplication.java
│   ├── config/            # CORS + WebClient beans
│   ├── controller/        # REST endpoints
│   ├── dto/                # Request/response payloads (incl. LLM output schema)
│   ├── exception/         # Global exception handling
│   ├── model/             # JPA entities (JobRequirement, CandidateResume, ScreeningResult)
│   ├── repository/        # Spring Data JPA repositories
│   └── service/
│       ├── PromptBuilderService.java     # ⭐ prompt engineering
│       ├── GeminiClientService.java      # ⭐ Gemini LLM API integration
│       ├── ResumeTextExtractorService.java
│       └── ScreeningService.java         # orchestrates the pipeline
└── src/main/resources/
    ├── application.properties
    └── static/             # HTML/CSS/JS frontend
        ├── index.html
        ├── css/style.css
        └── js/app.js
```

---

## 🔧 Configuration Reference (`application.properties`)

| Property | Default | Description |
|---|---|---|
| `gemini.api.key` | env `GEMINI_API_KEY` | Your Gemini API key |
| `gemini.api.model` | `gemini-2.5-flash` | Gemini model to call |
| `gemini.api.temperature` | `0.2` | Low temperature for consistent scoring |
| `spring.datasource.url` | `jdbc:h2:file:./data/resumedb` | H2 DB file location |
| `spring.servlet.multipart.max-file-size` | `10MB` | Max size per resume file |

---

## 🔮 Possible Extensions
- Swap H2 for PostgreSQL/MySQL by changing the datasource properties + driver dependency.
- Add authentication (Spring Security) for multi-recruiter usage.
- Add bulk resume ZIP upload.
- Add email notifications to shortlisted candidates.
- Stream Gemini responses for large batch screening.

---

## ⚠️ Notes
- Gemini model names change over time — if `gemini.api.model` becomes unavailable, update
  it to the latest model listed at https://ai.google.dev/gemini-api/docs/models.
- H2 is used here for a simple, self-contained deployment. For production scale, swap in
  PostgreSQL (add `postgresql` driver dependency and update `spring.datasource.*`).
