# MRI Frontend Rewrite Plan (Plan 2 of 3)

> Execute inline (subagent/workflow quota-blocked). Steps use `- [ ]`.

**Goal:** Replace the single-page `App.jsx` with a routed, user-language, page-per-feature frontend: login page → 7 feature pages, dropdowns instead of ID-typing, delete everywhere, MinIO image upload/preview, and business-logic-aware button states.

**Architecture:** `react-router-dom` HashRouter + `AppProvider` context (auth + shared lists + activity log + `runAction`) + extracted `ui.jsx` components + `AppLayout` shell + 8 page files. All requests via `lib/api.js` through the Vite proxy `/api`.

**Tech Stack:** React 18, Vite 8, react-router-dom 6, lucide-react.

## Global Constraints
- UI language = clinical/user Chinese. No Study/Series/UID/Token/Redis/Nacos/Feign/网关 in the UI.
- Raw IDs never typed by users — use dropdown `<select>` populated from context lists.
- Delete = confirm dialog (`ConfirmDialog`) → api → remove from list → activity log.
- Status-aware buttons: disable actions not valid for current status.
- Keep seed data as offline display fallback; interactions hit the backend.
- Preserve existing visual language (styles.css teal/dark-blue medical theme) — extend, don't replace.

## Files
- Modify: `mri-frontend/package.json` (+react-router-dom), `src/main.jsx`, `src/App.jsx` (router), `src/styles.css` (extend), `src/lib/api.js` (full method set), `src/lib/seed.js` (add clinicalDiagnosis/priority/impression to seed)
- Create: `src/lib/app-context.jsx`, `src/components/ui.jsx`, `src/components/AppLayout.jsx`
- Create pages: `src/pages/{LoginPage,DashboardPage,PatientsPage,ExamsPage,ImagesPage,ReportsPage,SettingsPage,ActivityPage}.jsx`

## Tasks

- [ ] **F1** package.json + react-router-dom; `npm install`
- [ ] **F2** `lib/api.js` — methods: login/logout/me; patients CRUD + page(keyword); exams CRUD + cancel/start/complete + page(status) + byPatient; schedules CRUD + listByExam; studies archive/delete + page + listSeries(byStudy); series delete; files upload(multipart)/delete/content(blob) + viewerManifest + cacheDemo; reports CRUD + submit/approve/reject/reopen/publish + page(status) + auditLogs; demoConfig; download(studyId,reason) + downloadLogs; contraindications list/create/delete; fileContent fetches blob with auth.
- [ ] **F3** `lib/app-context.jsx` — AppProvider: token/username/login/logout; shared lists patients/exams/studies/reports + refreshers; logs + log() + runAction(key,title,fn,onSuccess); helpers patientName/examLabel/studyLabel.
- [ ] **F4** `components/ui.jsx` — Button, TextField, SelectField(options {value,label}), TextAreaField, DataTable, StatusTag, SectionHeader, Metric, EmptyState, ConfirmDialog, PageHeader, SearchBar, FileDropzone.
- [ ] **F5** `components/AppLayout.jsx` (sidebar 7 items + topbar + RequireAuth + Outlet) + `App.jsx` (HashRouter routes) + `main.jsx`.
- [ ] **F6** LoginPage.
- [ ] **F7** DashboardPage (metrics + 流程 + 待办 + recent activity).
- [ ] **F8** PatientsPage (search+page+CRUD+delete) + patient detail (contraindications CRUD + exam history).
- [ ] **F9** ExamsPage (exams: status filter + create(patient dropdown) + cancel/start/complete + edit; schedules sub-section: create/delete by exam).
- [ ] **F10** ImagesPage (studies list w/ patient join + archive + delete; select study → series add/delete; select series → upload FileDropzone + image grid w/ real thumbnails via content blob + delete; 快速预览 button = viewerManifest+cacheDemo).
- [ ] **F11** ReportsPage (status filter + create(exam/study dropdown) + edit + delete(draft/rejected only) + submit/approve/reject(reason)/reopen/publish + audit-log timeline).
- [ ] **F12** SettingsPage (watermark + download toggle + reload).
- [ ] **F13** ActivityPage (global activity log, user-language).
- [ ] **F14** styles.css extensions (login card, page-header, confirm-dialog, search-bar, file-dropzone, image-thumb, audit-timeline, nav route active).
- [ ] **F15** `npm run build` + `npm audit` (0 vulns); Playwright screenshots if time.

## Self-Review
- All 8 pages present; every backend endpoint from Plan 1 has a UI surface (upload, content, by-patient, audit-logs, download-logs, reopen, cancel); delete on all record types; status-aware buttons; no technical terms in UI. ✓
