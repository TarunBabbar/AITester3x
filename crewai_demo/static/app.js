/* CrewAI QA Demo — frontend logic (two-phase: generate -> automate -> run) */

const $ = (sel) => document.querySelector(sel);

const urlInput = $("#url");
const reqInput = $("#requirements");
const generateBtn = $("#generate-btn");
const automateBtn = $("#automate-btn");
const selectAllBtn = $("#select-all-btn");
const runTestsBtn = $("#run-tests-btn");
const errorBox = $("#error-box");
const stepsCard = $("#steps-card");
const stepsList = $("#steps");
const testsCard = $("#tests-card");
const testList = $("#test-list");
const resultsCard = $("#results-card");

let currentRunId = null;
let allTestCases = [];

// ---------------------------------------------------------------------------
// Health check on load
// ---------------------------------------------------------------------------
async function checkHealth() {
  const dot = $(".dot");
  const text = $("#health-text");
  try {
    const res = await fetch("/api/health");
    const data = await res.json();
    dot.className = "dot " + (data.status === "ok" ? "ok" : "err");
    const node = data.node?.available ? "node ✓" : "node ✗";
    const npm = data.npm?.available ? "npm ✓" : "npm ✗";
    text.textContent = `${node} ${npm}`;
  } catch {
    dot.className = "dot err";
    text.textContent = "backend offline";
  }
}

// ---------------------------------------------------------------------------
// Pipeline steps rendering
// ---------------------------------------------------------------------------
function showSteps(steps) {
  stepsCard.classList.remove("hidden");
  stepsList.innerHTML = "";
  for (const s of steps) {
    const li = document.createElement("li");
    li.className = s.status;
    li.innerHTML = `<span class="name">${escapeHtml(s.step)}</span>
                    <span class="state">${escapeHtml(s.status)}</span>`;
    if (s.detail) {
      const detail = document.createElement("div");
      detail.className = "detail";
      detail.style.color = "var(--muted)";
      detail.style.fontSize = "0.8rem";
      detail.textContent = s.detail;
      li.appendChild(detail);
    }
    stepsList.appendChild(li);
  }
}

// ---------------------------------------------------------------------------
// Tabs
// ---------------------------------------------------------------------------
$("#tabs").addEventListener("click", (e) => {
  const btn = e.target.closest(".tab");
  if (!btn) return;
  document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
  document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
  btn.classList.add("active");
  $(`#${btn.dataset.tab}`).classList.add("active");
});

// ---------------------------------------------------------------------------
// Test case list
// ---------------------------------------------------------------------------
function renderTestCases(cases) {
  allTestCases = cases;
  testList.innerHTML = "";
  for (const tc of cases) {
    const item = document.createElement("div");
    item.className = "test-case";

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.dataset.tcId = tc.id;
    checkbox.checked = true;

    const body = document.createElement("div");
    body.className = "tc-body";

    const title = document.createElement("div");
    title.className = "tc-title";
    const id = document.createElement("span");
    id.className = "tc-id";
    id.textContent = tc.id;
    title.appendChild(id);
    const name = document.createElement("span");
    name.textContent = tc.title;
    title.appendChild(name);
    const prio = document.createElement("span");
    prio.className = "tc-priority " + (tc.priority || "P2");
    prio.textContent = tc.priority || "P2";
    title.appendChild(prio);
    body.appendChild(title);

    if (Array.isArray(tc.steps) && tc.steps.length) {
      const ol = document.createElement("ol");
      ol.className = "tc-steps";
      tc.steps.forEach((step) => {
        const li = document.createElement("li");
        li.textContent = step;
        ol.appendChild(li);
      });
      body.appendChild(ol);
    }
    if (tc.expected) {
      const exp = document.createElement("div");
      exp.className = "tc-expected";
      exp.innerHTML = `<strong>Expected:</strong> ${escapeHtml(tc.expected)}`;
      body.appendChild(exp);
    }

    item.appendChild(checkbox);
    item.appendChild(body);
    testList.appendChild(item);
  }
  testsCard.classList.remove("hidden");
}

function selectedIds() {
  return Array.from(testList.querySelectorAll("input[type=checkbox]:checked"))
    .map((cb) => cb.dataset.tcId);
}

// ---------------------------------------------------------------------------
// Phase 1: generate test cases
// ---------------------------------------------------------------------------
generateBtn.addEventListener("click", async () => {
  const url = urlInput.value.trim();
  if (!url) {
    showError("Enter a page URL first.");
    return;
  }

  generateBtn.disabled = true;
  errorBox.classList.add("hidden");
  testsCard.classList.add("hidden");
  resultsCard.classList.add("hidden");
  showSteps([{ step: "Submitting", status: "running", detail: "Starting pipeline…" }]);

  try {
    const res = await fetch("/api/generate-tests", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url, requirements: reqInput.value.trim() }),
    });
    const data = await res.json();
    showSteps(data.steps || []);

    if (data.status !== "success") {
      showError((data.steps || []).map((s) => `${s.step}: ${s.detail}`).join("\n"));
      return;
    }

    currentRunId = data.run_id;
    renderTestCases(data.test_cases || []);
    stepsCard.scrollIntoView({ behavior: "smooth", block: "nearest" });
  } catch (err) {
    showError("Backend unreachable. Is the app running?\n" + err.message);
  } finally {
    generateBtn.disabled = false;
  }
});

// ---------------------------------------------------------------------------
// Select all / none
// ---------------------------------------------------------------------------
selectAllBtn.addEventListener("click", () => {
  const checkboxes = testList.querySelectorAll("input[type=checkbox]");
  const allChecked = Array.from(checkboxes).every((cb) => cb.checked);
  checkboxes.forEach((cb) => (cb.checked = !allChecked));
});

// ---------------------------------------------------------------------------
// Phase 2: automate selected
// ---------------------------------------------------------------------------
automateBtn.addEventListener("click", async () => {
  const selected = selectedIds();
  if (!selected.length) {
    showError("Select at least one test case to automate.");
    return;
  }

  automateBtn.disabled = true;
  errorBox.classList.add("hidden");
  showSteps([{ step: "Automating", status: "running", detail: "Building POM + framework…" }]);

  try {
    const res = await fetch("/api/automate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ run_id: currentRunId, selected }),
    });
    const data = await res.json();
    showSteps(data.steps || []);

    if (data.status !== "success") {
      showError((data.steps || []).map((s) => `${s.step}: ${s.detail}`).join("\n"));
      return;
    }

    $("#pom .output").textContent = data.pom_code || "(no POM generated)";

    // Load framework files
    const filesOut = $("#framework .output");
    try {
      const filesRes = await fetch("/api/output");
      const filesData = await filesRes.json();
      // Show only files for this run (they live in output/<run_id>/)
      const runFiles = filesData.files.filter(
        (f) => f.name.startsWith(currentRunId + "/")
      );
      filesOut.textContent = runFiles.length
        ? runFiles.map((f) => `// FILE: ${f.name}\n${f.content}`).join("\n\n")
        : "(no framework files written)";
    } catch {
      filesOut.textContent = (data.framework_files || []).join("\n");
    }

    $("#run .output").textContent = "(run the tests to see output)";
    $("#run .output").className = "output";
    resultsCard.classList.remove("hidden");
    resultsCard.scrollIntoView({ behavior: "smooth", block: "nearest" });
  } catch (err) {
    showError("Backend unreachable. Is the app running?\n" + err.message);
  } finally {
    automateBtn.disabled = false;
  }
});

// ---------------------------------------------------------------------------
// Phase 2b: run the generated tests
// ---------------------------------------------------------------------------
runTestsBtn.addEventListener("click", async () => {
  if (!currentRunId) return;
  runTestsBtn.disabled = true;
  const out = $("#run .output");
  out.textContent = "Running npx playwright test — this can take a while…";
  out.className = "output";
  try {
    const res = await fetch("/api/run-tests", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ run_id: currentRunId }),
    });
    const data = await res.json();
    out.textContent = data.test_result?.output || "(no output)";
    out.className = "output";
    if (data.test_result?.success === true) out.classList.add("ok");
    else if (data.test_result?.success === false) out.classList.add("err");
  } catch (err) {
    out.textContent = "Failed to run tests: " + err.message;
    out.className = "output err";
  } finally {
    runTestsBtn.disabled = false;
  }
});

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function showError(msg) {
  errorBox.textContent = msg;
  errorBox.classList.remove("hidden");
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

checkHealth();
