const TOKEN_KEY = 'mri.frontend.token';
const USER_KEY = 'mri.frontend.user';
const DISPLAY_NAME_KEY = 'mri.frontend.displayName';
const ROLES_KEY = 'mri.frontend.roles';

export class ApiError extends Error {
  constructor(message, status, options) {
    super(message, options);
    this.name = 'ApiError';
    this.status = status;
  }
}

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
  if (session?.displayName) {
    localStorage.setItem(DISPLAY_NAME_KEY, session.displayName);
  }
  if (Array.isArray(session?.roles)) {
    localStorage.setItem(ROLES_KEY, JSON.stringify(session.roles));
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(DISPLAY_NAME_KEY);
  localStorage.removeItem(ROLES_KEY);
}

export function getStoredUser() {
  return localStorage.getItem(USER_KEY) || '';
}

export function getStoredSession() {
  let roles = [];
  try {
    roles = JSON.parse(localStorage.getItem(ROLES_KEY) || '[]');
  } catch {
    roles = [];
  }
  return {
    token: getToken(),
    username: getStoredUser(),
    displayName: localStorage.getItem(DISPLAY_NAME_KEY) || '',
    roles: Array.isArray(roles) ? roles : [],
  };
}

function parsePayload(text) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function httpStatusMessage(status) {
  const messages = {
    400: '提交的信息有误，请检查后重试',
    401: '登录状态已失效，请重新登录',
    403: '当前账号没有执行此操作的权限',
    404: '请求的内容不存在或已被删除',
    409: '数据状态已发生变化，请刷新后重试',
    413: '上传内容过大，请选择较小的文件',
    415: '提交的内容格式不受支持',
    422: '提交的信息不完整或格式有误',
    429: '操作过于频繁，请稍后重试',
    500: '系统处理失败，请稍后重试',
    502: '系统服务暂时不可用，请稍后重试',
    503: '系统服务暂时不可用，请稍后重试',
    504: '系统响应超时，请稍后重试',
  };
  return messages[status] || '操作未完成，请稍后重试';
}

function responseError(response, payload) {
  if (response.status === 401) {
    clearSession();
  }
  return new ApiError(payload?.message || httpStatusMessage(response.status), response.status);
}

async function userFriendlyFetch(path, options) {
  try {
    return await fetch(path, options);
  } catch (error) {
    throw new ApiError('无法连接服务器，请检查网络或稍后重试', 0, { cause: error });
  }
}

function authHeaders(extra = {}) {
  const headers = { ...(extra || {}) };
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

export async function apiRequest(path, options = {}) {
  const headers = authHeaders({ 'Content-Type': 'application/json', ...(options.headers || {}) });
  const response = await userFriendlyFetch(path, { ...options, headers });
  const payload = parsePayload(await response.text());
  if (!response.ok) {
    throw responseError(response, payload);
  }
  if (payload && payload.success === false) {
    throw new ApiError(payload.message || '操作未完成，请稍后重试', response.status);
  }
  return payload?.data ?? payload;
}

async function apiUpload(path, formData) {
  const headers = authHeaders();
  const response = await userFriendlyFetch(path, { method: 'POST', headers, body: formData });
  const payload = parsePayload(await response.text());
  if (!response.ok) {
    throw responseError(response, payload);
  }
  if (payload && payload.success === false) {
    throw new ApiError(payload.message || '上传失败，请稍后重试', response.status);
  }
  return payload?.data ?? payload;
}

async function apiBlob(path) {
  const headers = authHeaders();
  const response = await userFriendlyFetch(path, { headers });
  if (!response.ok) {
    throw responseError(response, null);
  }
  return response.blob();
}

export function downloadFileName(contentDisposition, fallback = '影像文件') {
  if (!contentDisposition) {
    return fallback;
  }
  const utf8 = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1].trim().replace(/^"|"$/g, ''));
    } catch {
      return fallback;
    }
  }
  const plain = contentDisposition.match(/filename\s*=\s*("?)([^";]+)\1/i);
  return plain?.[2]?.trim() || fallback;
}

export async function downloadAttachment(path, fallbackName = '影像文件') {
  const response = await userFriendlyFetch(path, { headers: authHeaders() });
  if (!response.ok) {
    const payload = parsePayload(await response.text());
    throw responseError(response, payload);
  }
  const blob = await response.blob();
  const fileName = downloadFileName(response.headers.get('Content-Disposition'), fallbackName);
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  try {
    anchor.href = objectUrl;
    anchor.download = fileName;
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    anchor.click();
  } finally {
    setTimeout(() => {
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
    }, 1000);
  }
  return fileName;
}

function toRecords(pageResult) {
  if (Array.isArray(pageResult)) {
    return pageResult;
  }
  return pageResult?.records || [];
}

export const api = {
  // 认证
  login: (body) => apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => apiRequest('/api/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  logout: () => apiRequest('/api/auth/logout', { method: 'POST' }),
  me: () => apiRequest('/api/auth/me'),

  // 患者
  patients: (page = 1, size = 10, keyword = '') =>
    apiRequest(`/api/patients?page=${page}&size=${size}${keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''}`).then(toRecords),
  patientPage: (page = 1, size = 10, keyword = '') =>
    apiRequest(`/api/patients?page=${page}&size=${size}${keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''}`),
  createPatient: (body) => apiRequest('/api/patients', { method: 'POST', body: JSON.stringify(body) }),
  updatePatient: (id, body) => apiRequest(`/api/patients/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deletePatient: (id) => apiRequest(`/api/patients/${id}`, { method: 'DELETE' }),
  contraindications: (patientId) => apiRequest(`/api/patients/${patientId}/contraindications`).then(toRecords),
  createContraindication: (patientId, body) => apiRequest(`/api/patients/${patientId}/contraindications`, { method: 'POST', body: JSON.stringify(body) }),
  deleteContraindication: (id) => apiRequest(`/api/patients/contraindications/${id}`, { method: 'DELETE' }),
  examHistory: (patientId) => apiRequest(`/api/patients/${patientId}/exam-history`).then(toRecords),
  myProfile: () => apiRequest('/api/patients/me'),
  createMyProfile: (body) => apiRequest('/api/patients/me', { method: 'POST', body: JSON.stringify(body) }),
  updateMyProfile: (body) => apiRequest('/api/patients/me', { method: 'PUT', body: JSON.stringify(body) }),

  // 检查申请与排程
  exams: (page = 1, size = 10, status = '') =>
    apiRequest(`/api/exams?page=${page}&size=${size}${status ? `&status=${encodeURIComponent(status)}` : ''}`).then(toRecords),
  createExam: (body) => apiRequest('/api/exams', { method: 'POST', body: JSON.stringify(body) }),
  updateExam: (id, body) => apiRequest(`/api/exams/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  cancelExam: (id) => apiRequest(`/api/exams/${id}/cancel`, { method: 'POST' }),
  examRisk: (id) => apiRequest(`/api/exams/${id}/risk`),
  startExam: (id, confirmHighRisk = false) =>
    apiRequest(`/api/exams/${id}/start?confirmHighRisk=${confirmHighRisk}`, { method: 'POST' }),
  completeExam: (id) => apiRequest(`/api/exams/${id}/complete`, { method: 'POST' }),
  deleteExam: (id) => apiRequest(`/api/exams/${id}`, { method: 'DELETE' }),
  byPatient: (patientId) => apiRequest(`/api/exams/by-patient/${patientId}`).then(toRecords),
  schedules: (examOrderId) => apiRequest(`/api/exams/${examOrderId}/schedules`).then(toRecords),
  createSchedule: (body) => apiRequest('/api/exams/schedules', { method: 'POST', body: JSON.stringify(body) }),
  deleteSchedule: (id) => apiRequest(`/api/exams/schedules/${id}`, { method: 'DELETE' }),
  myExams: () => apiRequest('/api/exams/mine').then(toRecords),

  // 影像归档（图像管理）
  studies: (page = 1, size = 10, keyword = '') =>
    apiRequest(`/api/images/studies?page=${page}&size=${size}${keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''}`).then(toRecords),
  archiveStudy: (body) => apiRequest('/api/images/studies', { method: 'POST', body: JSON.stringify(body) }),
  deleteStudy: (id) => apiRequest(`/api/images/studies/${id}`, { method: 'DELETE' }),
  series: (studyId) => apiRequest(`/api/images/studies/${studyId}/series`).then(toRecords),
  createSeries: (studyId, body) => apiRequest(`/api/images/studies/${studyId}/series`, { method: 'POST', body: JSON.stringify(body) }),
  deleteSeries: (id) => apiRequest(`/api/images/series/${id}`, { method: 'DELETE' }),
  uploadFile: (studyId, seriesId, file) => {
    const form = new FormData();
    form.append('seriesId', String(seriesId));
    form.append('file', file);
    return apiUpload(`/api/images/studies/${studyId}/files`, form);
  },
  deleteFile: (id) => apiRequest(`/api/images/files/${id}`, { method: 'DELETE' }),
  fileContent: (id) => apiBlob(`/api/images/files/${id}/content`),
  viewerManifest: (studyId) => apiRequest(`/api/images/studies/${studyId}/viewer-manifest`),
  cacheDemo: (studyId) => apiRequest(`/api/images/studies/${studyId}/cache-demo`),
  downloadFile: (fileId, reason) =>
    downloadAttachment(`/api/images/files/${fileId}/download?reason=${encodeURIComponent(reason || '')}`, `影像-${fileId}`),
  downloadStudy: (studyId, reason) =>
    downloadAttachment(`/api/images/studies/${studyId}/download?reason=${encodeURIComponent(reason || '')}&transport=browser`, `Study-${studyId}-影像.zip`),
  downloadLogs: (studyId) => apiRequest(`/api/images/studies/${studyId}/download-logs`).then(toRecords),
  myStudies: () => apiRequest('/api/images/mine/studies').then(toRecords).then((records) =>
    records.map((item) => ({ ...item.study, fileCount: item.fileCount, reportPublished: item.reportPublished }))),
  myViewerManifest: (studyId) => apiRequest(`/api/images/mine/studies/${studyId}/viewer-manifest`),
  myFileContent: (id) => apiBlob(`/api/images/mine/files/${id}/content`),
  myDownloadFile: (id) => downloadAttachment(`/api/images/mine/files/${id}/download`, `影像-${id}`),
  myDownloadStudy: (studyId) =>
    downloadAttachment(`/api/images/mine/studies/${studyId}/download?transport=browser`, `Study-${studyId}-影像.zip`),

  // 诊断报告
  reports: (page = 1, size = 10, status = '') =>
    apiRequest(`/api/reports?page=${page}&size=${size}${status ? `&status=${encodeURIComponent(status)}` : ''}`).then(toRecords),
  createReport: (body) => apiRequest('/api/reports', { method: 'POST', body: JSON.stringify(body) }),
  updateReport: (id, body) => apiRequest(`/api/reports/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteReport: (id) => apiRequest(`/api/reports/${id}`, { method: 'DELETE' }),
  submitReport: (id) => apiRequest(`/api/reports/${id}/submit`, { method: 'POST' }),
  approveReport: (id) => apiRequest(`/api/reports/${id}/approve`, { method: 'POST' }),
  rejectReport: (id, reason) =>
    apiRequest(`/api/reports/${id}/reject?reason=${encodeURIComponent(reason || '退回修改')}`, { method: 'POST' }),
  reopenReport: (id) => apiRequest(`/api/reports/${id}/reopen`, { method: 'POST' }),
  publishReport: (id) => apiRequest(`/api/reports/${id}/publish`, { method: 'POST' }),
  auditLogs: (id) => apiRequest(`/api/reports/${id}/audit-logs`).then(toRecords),
  myReports: () => apiRequest('/api/reports/mine').then(toRecords),

  // 系统设置
  demoConfig: () => apiRequest('/api/images/demo/config'),
};
