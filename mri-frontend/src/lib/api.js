const TOKEN_KEY = 'mri.frontend.token';
const USER_KEY = 'mri.frontend.user';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function saveSession(session) {
  if (session?.token) {
    localStorage.setItem(TOKEN_KEY, session.token);
  }
  if (session?.username) {
    localStorage.setItem(USER_KEY, session.username);
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getStoredUser() {
  return localStorage.getItem(USER_KEY) || '未登录';
}

function parsePayload(text) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

export async function apiRequest(path, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(path, {
    ...options,
    headers,
  });
  const payload = parsePayload(await response.text());

  if (!response.ok) {
    throw new Error(payload?.message || `HTTP ${response.status}`);
  }
  if (payload && payload.success === false) {
    throw new Error(payload.message || '接口返回业务错误');
  }
  return payload?.data ?? payload;
}

export const api = {
  login: (body) => apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  logout: () => apiRequest('/api/auth/logout', { method: 'POST' }),
  me: () => apiRequest('/api/auth/me'),
  patients: () => apiRequest('/api/patients?page=1&size=10'),
  createPatient: (body) => apiRequest('/api/patients', { method: 'POST', body: JSON.stringify(body) }),
  createExam: (body) => apiRequest('/api/exams', { method: 'POST', body: JSON.stringify(body) }),
  startExam: (id) => apiRequest(`/api/exams/${id}/start`, { method: 'POST' }),
  completeExam: (id) => apiRequest(`/api/exams/${id}/complete`, { method: 'POST' }),
  createSchedule: (body) => apiRequest('/api/exams/schedules', { method: 'POST', body: JSON.stringify(body) }),
  archiveStudy: (body) => apiRequest('/api/images/studies', { method: 'POST', body: JSON.stringify(body) }),
  createSeries: (studyId, body) => apiRequest(`/api/images/studies/${studyId}/series`, { method: 'POST', body: JSON.stringify(body) }),
  createFile: (studyId, body) => apiRequest(`/api/images/studies/${studyId}/files`, { method: 'POST', body: JSON.stringify(body) }),
  viewerManifest: (studyId) => apiRequest(`/api/images/studies/${studyId}/viewer-manifest`),
  cacheDemo: (studyId) => apiRequest(`/api/images/studies/${studyId}/cache-demo`),
  demoConfig: () => apiRequest('/api/images/demo/config'),
  createReport: (body) => apiRequest('/api/reports', { method: 'POST', body: JSON.stringify(body) }),
  submitReport: (id) => apiRequest(`/api/reports/${id}/submit`, { method: 'POST' }),
  approveReport: (id) => apiRequest(`/api/reports/${id}/approve`, { method: 'POST' }),
  publishReport: (id) => apiRequest(`/api/reports/${id}/publish`, { method: 'POST' }),
};
