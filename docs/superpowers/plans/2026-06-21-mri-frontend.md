# MRI Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Chinese-first frontend workbench for the existing hospital MRI image information management microservice system.

**Architecture:** Create a standalone `mri-frontend` React + Vite app that talks to the existing Spring Cloud Gateway at `http://localhost:8080/api`. The UI is an operational workbench, not a landing page: it covers login, patient records, MRI exam orders, schedules, Study/Series/Image metadata, viewer manifest, report approval/publishing, Redis cache demo, Nacos config/register demo, and operation logs.

**Tech Stack:** React 18, Vite 5, vanilla CSS modules by convention in one `src/styles.css`, browser `fetch`, localStorage token persistence, JavaScript runtime checks, npm build verification.

---

## File Structure

- Create `mri-frontend/package.json`: npm scripts and dependencies.
- Create `mri-frontend/index.html`: root HTML shell with Chinese title.
- Create `mri-frontend/vite.config.js`: dev server on port `5173`, proxy `/api` to `http://localhost:8080`.
- Create `mri-frontend/src/main.jsx`: React bootstrap.
- Create `mri-frontend/src/App.jsx`: page composition, state, API actions, demo workflow.
- Create `mri-frontend/src/lib/api.js`: gateway client, token persistence, normalized response handling.
- Create `mri-frontend/src/lib/seed.js`: Chinese seed data shown when backend is not running.
- Create `mri-frontend/src/styles.css`: full UI system and responsive layout.
- Modify `README.md`: add frontend module and run instructions.
- Modify `docs/product-manual.md`: add frontend usage instructions for demo operators.
- Modify `.gitignore`: ignore frontend build/dependency output.

## Design System Extracted From Concept

- Visible language: Chinese labels first; English only where it is an interface term already used by the project, such as `Bearer Token`, `Study`, `Series`, `Image`, `Redis`, `Nacos`.
- Screen type: operational workbench with left navigation, top service status, workflow cards, tables, forms, right-side operation log, Redis/Nacos panel.
- Palette: white and light gray surfaces, deep navy sidebar `#06324a`, teal action `#007c89`, green success `#16a34a`, amber warning `#b7791f`, red error `#dc2626`.
- Typography: system UI stack, compact table text, no viewport-scaled fonts, no negative letter spacing.
- Containers: panels and tables, no marketing hero, no decorative orbs, no unrelated hospital sections.
- Core workflow: login stores JWT, all protected calls use gateway `/api/**`, UI logs success/error, seed data keeps the interface usable when backend is unavailable.

---

### Task 1: Frontend Scaffold

**Files:**
- Create: `mri-frontend/package.json`
- Create: `mri-frontend/index.html`
- Create: `mri-frontend/vite.config.js`
- Create: `mri-frontend/src/main.jsx`

- [ ] **Step 1: Create npm project metadata**

```json
{
  "name": "mri-frontend",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vite build",
    "preview": "vite preview --host 0.0.0.0"
  },
  "dependencies": {
    "@vitejs/plugin-react": "^4.3.4",
    "vite": "^5.4.21",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "lucide-react": "^0.468.0"
  },
  "devDependencies": {}
}
```

- [ ] **Step 2: Create HTML shell**

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>MRI影像信息管理系统</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
```

- [ ] **Step 3: Configure Vite gateway proxy**

```js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 4: Bootstrap React**

```jsx
import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import './styles.css';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
```

- [ ] **Step 5: Verify scaffold**

Run: `npm install` inside `mri-frontend`, then `npm run build`.
Expected: Vite build succeeds and writes `mri-frontend/dist`.

---

### Task 2: Gateway API Client

**Files:**
- Create: `mri-frontend/src/lib/api.js`

- [ ] **Step 1: Implement token storage and request helper**

```js
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

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(payload?.message || `HTTP ${response.status}`);
  }
  if (payload && payload.code && payload.code !== 200) {
    throw new Error(payload.message || '接口返回业务错误');
  }
  return payload?.data ?? payload;
}

export const api = {
  login: (body) => apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  logout: () => apiRequest('/api/auth/logout', { method: 'POST' }),
  patients: () => apiRequest('/api/patients?page=1&size=10'),
  createPatient: (body) => apiRequest('/api/patients', { method: 'POST', body: JSON.stringify(body) }),
  createExam: (body) => apiRequest('/api/exams', { method: 'POST', body: JSON.stringify(body) }),
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
```

- [ ] **Step 2: Verify client syntax**

Run: `npm run build`.
Expected: no import or syntax errors.

---

### Task 3: Seed Data And Main Workbench

**Files:**
- Create: `mri-frontend/src/lib/seed.js`
- Create: `mri-frontend/src/App.jsx`

- [ ] **Step 1: Add Chinese seed data**

```js
export const seedPatients = [
  { id: 1, patientNo: 'P20260621001', name: '张明', gender: '男', birthDate: '1980-01-01', phone: '138****0001' },
  { id: 2, patientNo: 'P20260621002', name: '李华', gender: '女', birthDate: '1974-09-18', phone: '139****0002' },
];

export const seedExams = [
  { id: 1, patientId: 1, patientName: '张明', examItem: '头颅MRI平扫', priority: '普通', status: '已完成' },
  { id: 2, patientId: 2, patientName: '李华', examItem: '腰椎MRI增强', priority: '加急', status: '待检查' },
];

export const seedStudies = [
  { id: 1, examOrderId: 1, studyInstanceUid: '1.2.840.113619.20260621.001', description: '头颅MRI平扫', status: '已归档' },
];
```

- [ ] **Step 2: Implement app composition**

Implement `App.jsx` with state for token, selected navigation, forms, patients, exams, studies, reports, viewer manifest, config result, and operation logs. Include these visible panels: left navigation, top status, login card, workflow cards, patient form/table, exam form/table, study archive form/table, viewer manifest preview, report form/actions, Redis/Nacos/config demo, operation log.

- [ ] **Step 3: Verify build**

Run: `npm run build`.
Expected: React compiles and the rendered UI contains only MRI information-management content.

---

### Task 4: Styling And Responsive Layout

**Files:**
- Create: `mri-frontend/src/styles.css`

- [ ] **Step 1: Add CSS tokens and app shell**

```css
:root {
  color-scheme: light;
  font-family: "Microsoft YaHei", "Segoe UI", Arial, sans-serif;
  background: #f3f6f8;
  color: #152238;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
}
```

- [ ] **Step 2: Add full UI styling**

Add sidebar, header, workflow cards, tables, forms, buttons, status tags, preview thumbnails, log rows, Redis/Nacos panels, and mobile layout. Use stable dimensions for navigation, buttons, thumbnails, tables, and status tags.

- [ ] **Step 3: Verify responsive layout**

Run: `npm run build`, then `npm run dev` and inspect `http://localhost:5173` at desktop and mobile widths.
Expected: no text overlap, no unrelated content, all primary actions remain visible.

---

### Task 5: Documentation And Git Hygiene

**Files:**
- Modify: `.gitignore`
- Modify: `README.md`
- Modify: `docs/product-manual.md`

- [ ] **Step 1: Ignore frontend output**

```gitignore
mri-frontend/node_modules/
mri-frontend/dist/
```

- [ ] **Step 2: Add README frontend commands**

Add:

```markdown
## 前端界面

前端模块位于 `mri-frontend`，默认通过 Vite 代理访问网关 `http://localhost:8080/api/**`。

```powershell
cd mri-frontend
npm install
npm run dev
```

浏览器访问：`http://localhost:5173`
```

- [ ] **Step 3: Add product manual frontend steps**

Add a section explaining how demo operators use the frontend to login, create patient, create MRI exam, archive Study, view manifest, submit/approve/publish report, and run Redis/Nacos/config checks.

- [ ] **Step 4: Verify repository state**

Run:

```powershell
npm run build
mvn clean test
git status --short
```

Expected: frontend build succeeds, backend tests pass, only intentional source/doc files are changed.

---

## Self-Review

- Spec coverage: The plan adds a frontend and keeps content constrained to the existing MRI system.
- Placeholder scan: No unfinished marker, unrelated module, marketing section, or fake clinical diagnosis is included.
- Type consistency: API paths match gateway routes and controller methods already present in the backend.
