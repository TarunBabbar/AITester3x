import { useState, useRef, useEffect } from 'react';
import { fetchStats, sendQuery } from './api';

const PIPELINE_STEPS = [
  { id: 'query', label: 'User Query', icon: '❓' },
  { id: 'embedding', label: 'Query Embedding', icon: '🧬' },
  { id: 'retrieval', label: 'Semantic Retrieval (Top 4)', icon: '🎯' },
  { id: 'prompt', label: 'Context Assembly (Prompt)', icon: '📋' },
  { id: 'answer', label: 'LLM Response', icon: '🤖' },
];

export default function App() {
  const [query, setQuery] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState({ total_chunks: 0, sources: [] });
  const [expandedSteps, setExpandedSteps] = useState({});
  const [activeStep, setActiveStep] = useState(null);
  const answerRef = useRef(null);

  useEffect(() => {
    fetchStats().then(setStats).catch(() => {});
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!query.trim() || loading) return;

    setLoading(true);
    setResult(null);
    setExpandedSteps({ query: true });
    setActiveStep('query');

    try {
      const data = await sendQuery(query);
      setResult(data);

      // Expand steps one by one for visual effect
      const stepIds = ['query', 'embedding', 'retrieval', 'prompt', 'answer'];
      for (let i = 0; i < stepIds.length; i++) {
        await new Promise((r) => setTimeout(r, 200));
        setExpandedSteps((prev) => ({ ...prev, [stepIds[i]]: true }));
        setActiveStep(stepIds[i]);
      }

      // Refresh stats
      const newStats = await fetchStats();
      setStats(newStats);
    } catch (err) {
      setResult({ error: err.message });
    } finally {
      setLoading(false);
      setActiveStep(null);
    }
  };

  const toggleStep = (id) => {
    setExpandedSteps((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <div className="app">
      {/* Header */}
      <header className="header">
        <h1>🔍 RAG Explorer</h1>
        <p>Visually explore every stage of Retrieval-Augmented Generation</p>
      </header>

      {/* Stats */}
      <div className="stats-bar">
        <div className="stat-card">
          <span className="icon">📚</span>
          <span className="value">{stats.total_chunks}</span>
          <span className="label">chunks indexed</span>
        </div>
        <div className="stat-card">
          <span className="icon">📄</span>
          <span className="value">{stats.sources.length}</span>
          <span className="label">documents</span>
        </div>
        {stats.sources.map((s) => (
          <div className="stat-card" key={s}>
            <span className="icon">📎</span>
            <span className="value">{s}</span>
          </div>
        ))}
      </div>

      {/* Search */}
      <form className="search-section" onSubmit={handleSubmit}>
        <div className="search-box">
          <input
            type="text"
            placeholder="Ask a question about your documents..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            disabled={loading}
          />
          <button type="submit" disabled={loading || !query.trim()} className={loading ? 'loading' : ''}>
            {loading ? '⏳' : 'Ask'}
          </button>
        </div>
      </form>

      {/* Results */}
      {result && result.error && (
        <div className="step" style={{ borderColor: '#ef4444' }}>
          <div className="step-header">
            <span style={{ fontSize: '1.2rem' }}>⚠️</span>
            <h3>Error</h3>
          </div>
          <div className="step-content">
            <p style={{ color: '#f87171' }}>{result.error}</p>
          </div>
        </div>
      )}

      {result && !result.error && (
        <div className="pipeline">
          {/* Step 1: User Query */}
          <Step
            step={PIPELINE_STEPS[0]}
            isActive={activeStep === 'query'}
            isExpanded={expandedSteps.query}
            onToggle={() => toggleStep('query')}
          >
            <QueryDisplay text={result.query} />
          </Step>

          {/* Step 2: Query Embedding */}
          <Step
            step={PIPELINE_STEPS[1]}
            isActive={activeStep === 'embedding'}
            isExpanded={expandedSteps.embedding}
            onToggle={() => toggleStep('embedding')}
          >
            <EmbeddingPreview embedding={result.query_embedding} />
          </Step>

          {/* Step 3: Retrieved Chunks */}
          <Step
            step={PIPELINE_STEPS[2]}
            isActive={activeStep === 'retrieval'}
            isExpanded={expandedSteps.retrieval}
            onToggle={() => toggleStep('retrieval')}
          >
            {result.retrieved_chunks.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                No relevant chunks found. The vector store may be empty.
              </p>
            ) : (
              <div className="chunks-grid">
                {result.retrieved_chunks.map((chunk, i) => (
                  <div className="chunk-card" key={i}>
                    <div className="chunk-meta">
                      <span className="chunk-tag source">
                        📄 {chunk.metadata.source}
                      </span>
                      <span className="chunk-tag page">
                        📄 Page {chunk.metadata.page}
                      </span>
                      <span className="chunk-tag score">
                        🎯 Score: {(chunk.score * 100).toFixed(1)}%
                      </span>
                    </div>
                    <div className="chunk-text">{chunk.text}</div>
                  </div>
                ))}
              </div>
            )}
          </Step>

          {/* Step 4: Final Prompt */}
          <Step
            step={PIPELINE_STEPS[3]}
            isActive={activeStep === 'prompt'}
            isExpanded={expandedSteps.prompt}
            onToggle={() => toggleStep('prompt')}
          >
            <PromptDisplay prompt={result.final_prompt} />
          </Step>

          {/* Step 5: Answer */}
          <Step
            step={PIPELINE_STEPS[4]}
            isActive={activeStep === 'answer'}
            isExpanded={expandedSteps.answer}
            onToggle={() => toggleStep('answer')}
          >
            <AnswerDisplay answer={result.answer} />
          </Step>
        </div>
      )}

      {!result && !loading && (
        <div className="empty-state">
          <div className="icon">🔬</div>
          <h3>Explore the RAG Pipeline</h3>
          <p>
            Add PDF files to the <code>data/pdf</code> folder — they will be
            automatically ingested. Then ask a question to see every stage of
            the Retrieval-Augmented Generation process in action.
          </p>
        </div>
      )}

      {loading && (
        <div className="step active" style={{ marginTop: 20 }}>
          <div className="step-header">
            <div className="typing-indicator">
              <span></span><span></span><span></span>
            </div>
            <h3 style={{ marginLeft: 8 }}>Processing your query through the RAG pipeline...</h3>
          </div>
        </div>
      )}
    </div>
  );
}

/* ── Step Component ──────────────────────────────────────── */
function Step({ step, isActive, isExpanded, onToggle, children }) {
  const badge = isActive ? 'active' : isExpanded ? 'done' : 'pending';
  const badgeLabel = isActive ? 'Processing' : isExpanded ? 'Complete' : 'Pending';

  return (
    <div className={`step ${isExpanded ? 'active' : ''}`}>
      <div className="step-header" onClick={onToggle}>
        <span className="step-number">{step.icon}</span>
        <h3>{step.label}</h3>
        <span className={`step-badge ${badge}`}>{badgeLabel}</span>
      </div>
      {isExpanded && <div className="step-content">{children}</div>}
    </div>
  );
}

/* ── Sub-components ──────────────────────────────────────── */
function QueryDisplay({ text }) {
  return (
    <div className="query-display">
      <div className="label">User Question</div>
      <div className="text">{text}</div>
    </div>
  );
}

function EmbeddingPreview({ embedding }) {
  if (!embedding || embedding.length === 0) return null;

  const maxVal = Math.max(...embedding.map(Math.abs), 0.01);

  return (
    <div className="embedding-preview">
      <div className="label">Query Embedding Vector (first {embedding.length} dimensions)</div>
      <div className="embedding-bars">
        {embedding.map((val, i) => {
          const heightPct = Math.max(4, (Math.abs(val) / maxVal) * 100);
          return (
            <div
              key={i}
              className="embedding-bar"
              style={{
                height: `${heightPct}%`,
                background: val >= 0 ? 'var(--accent)' : 'var(--pink)',
              }}
              title={`dim ${i}: ${val.toFixed(4)}`}
            />
          );
        })}
      </div>
      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 6 }}>
        Dimensions: {embedding.length} · Bars show relative magnitude per dimension
      </div>
    </div>
  );
}

function PromptDisplay({ prompt }) {
  return (
    <div className="prompt-block">
      <div className="label">Final Prompt Sent to LLM</div>
      <pre>{prompt}</pre>
    </div>
  );
}

function AnswerDisplay({ answer }) {
  return (
    <div className="answer-block">
      <div className="label">Generated Answer</div>
      <div className="answer-text">{answer}</div>
    </div>
  );
}