const API_BASE = '/api';

let jobs = [];
let currentJobId = null;
let currentResults = [];

const jobForm = document.getElementById('jobForm');
const uploadSection = document.getElementById('uploadSection');
const resultsSection = document.getElementById('resultsSection');
const jobSelect = document.getElementById('jobSelect');
const activeJobTitle = document.getElementById('activeJobTitle');
const screenBtn = document.getElementById('screenBtn');
const screenBtnText = document.getElementById('screenBtnText');
const loadingIndicator = document.getElementById('loadingIndicator');
const uploadError = document.getElementById('uploadError');
const resultsList = document.getElementById('resultsList');
const verdictFilter = document.getElementById('verdictFilter');
const detailModal = document.getElementById('detailModal');
const modalContent = document.getElementById('modalContent');
const modalClose = document.getElementById('modalClose');

// ---------------- Job creation ----------------
jobForm.addEventListener('submit', async (e) => {
  e.preventDefault();

  const payload = {
    title: document.getElementById('jobTitle').value.trim(),
    description: document.getElementById('jobDescription').value.trim(),
    requiredSkills: document.getElementById('requiredSkills').value.trim(),
    niceToHaveSkills: document.getElementById('niceToHaveSkills').value.trim(),
    minExperienceYears: document.getElementById('minExperience').value || null,
    qualification: document.getElementById('qualification').value.trim()
  };

  try {
    const res = await fetch(`${API_BASE}/jobs`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (!res.ok) {
      const err = await res.json();
      throw new Error(JSON.stringify(err));
    }
    const job = await res.json();
    await loadJobs();
    selectJob(job.id);
    uploadSection.style.display = 'block';
    uploadSection.scrollIntoView({ behavior: 'smooth' });
  } catch (err) {
    alert('Could not create job: ' + err.message);
  }
});

async function loadJobs() {
  const res = await fetch(`${API_BASE}/jobs`);
  jobs = await res.json();
  jobSelect.innerHTML = jobs.map(j => `<option value="${j.id}">${escapeHtml(j.title)}</option>`).join('');
}

function selectJob(jobId) {
  currentJobId = jobId;
  jobSelect.value = jobId;
  const job = jobs.find(j => j.id === jobId);
  if (job) activeJobTitle.textContent = job.title;
}

jobSelect.addEventListener('change', () => {
  selectJob(Number(jobSelect.value));
  resultsSection.style.display = 'none';
});

// ---------------- Resume upload & screening ----------------
screenBtn.addEventListener('click', async () => {
  const fileInput = document.getElementById('resumeFiles');
  const files = fileInput.files;

  uploadError.style.display = 'none';

  if (!currentJobId) {
    showError('Please create or select a job first.');
    return;
  }
  if (!files || files.length === 0) {
    showError('Please choose at least one resume file (PDF, DOCX, or TXT).');
    return;
  }

  const formData = new FormData();
  for (const file of files) {
    formData.append('files', file);
  }

  setLoading(true);

  try {
    const res = await fetch(`${API_BASE}/screening/jobs/${currentJobId}/upload`, {
      method: 'POST',
      body: formData
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Screening failed. Check your Gemini API key configuration.');
    }

    await loadResults(currentJobId);
    fileInput.value = '';
  } catch (err) {
    showError(err.message);
  } finally {
    setLoading(false);
  }
});

function setLoading(isLoading) {
  screenBtn.disabled = isLoading;
  loadingIndicator.style.display = isLoading ? 'block' : 'none';
  screenBtnText.textContent = isLoading ? 'Analyzing...' : 'Screen Resumes with AI';
}

function showError(message) {
  uploadError.textContent = message;
  uploadError.style.display = 'block';
}

// ---------------- Results ----------------
async function loadResults(jobId) {
  const res = await fetch(`${API_BASE}/screening/jobs/${jobId}/results`);
  currentResults = await res.json();
  resultsSection.style.display = 'block';
  renderResults();
  resultsSection.scrollIntoView({ behavior: 'smooth' });
}

verdictFilter.addEventListener('change', renderResults);

function renderResults() {
  const filter = verdictFilter.value;
  const filtered = filter === 'ALL'
    ? currentResults
    : currentResults.filter(r => r.verdict === filter);

  if (filtered.length === 0) {
    resultsList.innerHTML = `<div class="empty-state">No candidates match this filter yet.</div>`;
    return;
  }

  resultsList.innerHTML = filtered.map((r, idx) => {
    const score = r.matchScore ?? 0;
    const ringColor = scoreColor(score);
    const matchedCount = (r.matchedSkills || '').split(',').filter(s => s.trim()).length;
    const missingCount = (r.missingSkills || '').split(',').filter(s => s.trim()).length;

    return `
      <div class="result-row" data-idx="${currentResults.indexOf(r)}">
        <div class="score-ring" style="background: conic-gradient(${ringColor} ${score * 3.6}deg, var(--bg-alt) 0deg); ">
          <span>${score}</span>
        </div>
        <div>
          <div class="candidate-name">${escapeHtml(r.resume?.candidateName || 'Unknown Candidate')}</div>
          <div class="candidate-meta">${matchedCount} skills matched · ${missingCount} gaps · ${r.candidateExperienceYears ?? '?'} yrs exp</div>
        </div>
        <div class="verdict-badge verdict-${r.verdict}">${verdictLabel(r.verdict)}</div>
      </div>
    `;
  }).join('');

  document.querySelectorAll('.result-row').forEach(row => {
    row.addEventListener('click', () => openDetail(currentResults[Number(row.dataset.idx)]));
  });
}

function scoreColor(score) {
  if (score >= 80) return 'var(--green)';
  if (score >= 50) return 'var(--accent)';
  return 'var(--coral)';
}

function verdictLabel(v) {
  return ({
    STRONGLY_RECOMMENDED: 'Strongly Recommended',
    RECOMMENDED: 'Recommended',
    NOT_RECOMMENDED: 'Not Recommended'
  })[v] || v;
}

function openDetail(result) {
  const matched = (result.matchedSkills || '').split(',').map(s => s.trim()).filter(Boolean);
  const missing = (result.missingSkills || '').split(',').map(s => s.trim()).filter(Boolean);

  modalContent.innerHTML = `
    <h2 style="margin-top:0;">${escapeHtml(result.resume?.candidateName || 'Unknown Candidate')}</h2>
    <p class="muted">${escapeHtml(result.resume?.fileName || '')}</p>

    <div class="modal-section">
      <h4>Match Score</h4>
      <div style="font-family: var(--font-mono); font-size: 1.6rem; color: ${scoreColor(result.matchScore)};">
        ${result.matchScore} / 100
        <span class="verdict-badge verdict-${result.verdict}" style="margin-left:10px; font-size: 0.7rem;">${verdictLabel(result.verdict)}</span>
      </div>
    </div>

    <div class="modal-section">
      <h4>AI Summary</h4>
      <p>${escapeHtml(result.summary || 'No summary available.')}</p>
    </div>

    <div class="modal-section">
      <h4>Matched Skills</h4>
      <div class="tag-group">
        ${matched.length ? matched.map(s => `<span class="tag tag-matched">${escapeHtml(s)}</span>`).join('') : '<span class="muted">None detected</span>'}
      </div>
    </div>

    <div class="modal-section">
      <h4>Missing Skills</h4>
      <div class="tag-group">
        ${missing.length ? missing.map(s => `<span class="tag tag-missing">${escapeHtml(s)}</span>`).join('') : '<span class="muted">None — full coverage</span>'}
      </div>
    </div>

    <div class="modal-section">
      <h4>Candidate Experience</h4>
      <p>${result.candidateExperienceYears ?? 'Not specified'} years</p>
    </div>
  `;

  detailModal.style.display = 'flex';
}

modalClose.addEventListener('click', () => detailModal.style.display = 'none');
detailModal.addEventListener('click', (e) => {
  if (e.target === detailModal) detailModal.style.display = 'none';
});

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ---------------- Init ----------------
loadJobs();
