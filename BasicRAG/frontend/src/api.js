const API_BASE = '/api';

export async function fetchStats() {
  const res = await fetch(`${API_BASE}/stats`);
  if (!res.ok) throw new Error('Failed to fetch stats');
  return res.json();
}

export async function sendQuery(query) {
  const res = await fetch(`${API_BASE}/query`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) throw new Error('Failed to process query');
  return res.json();
}

export async function reindex() {
  const res = await fetch(`${API_BASE}/reindex`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to reindex');
  return res.json();
}