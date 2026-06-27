# MRI 前端重构与影像管理设计文档

日期：2026-06-26
状态：已确认，待实现

## 1. 目标

基于现有分布式微服务演示系统，对前端与相关后端做四方面改造：

1. 界面全部改为用户/临床语言，移除技术术语（Study/Series/UID/Bearer Token/Redis/Nacos/Feign/网关 等）。
2. 每个功能拆成单独页面，从登录页开始（react-router）。
3. 记录提供删除功能（患者/排程/影像检查/序列/影像文件/报告删除；检查申请用「取消」）。
4. 新增 MinIO 对象存储影像管理：医生完成检查后上传图像，按病人关联，地址入库。
5. 优化业务逻辑：状态守卫、数据模型补全、级联删除、操作联动、暴露已有未开放功能（含真实检查历史）。

功能不变（后端能力保留），只是呈现方式与业务严谨性提升。

## 2. 页面结构（8 页 + 路由，HashRouter）

| 路由 | 页面 | 内容 |
|---|---|---|
| `/login` | 登录 | 独立整页居中卡片，演示账号提示，登录后跳 `/`。未登录访问任意页 → 跳登录。 |
| `/` | 工作台 | 数量卡片（患者/检查/影像/报告）+ 检查流程示意（登记→申请→检查→归档→报告）+ 待办（待检查/待归档/待审核，可点击带筛选跳转）+ 最近操作记录。 |
| `/patients` | 患者档案 | 列表（搜索：姓名/编号 + 分页）+ 新增 + 删除 + 编辑。点击患者进入详情（禁忌症增删改 + 检查历史）。 |
| `/exams` | 检查申请（含排程） | 检查申请列表（状态筛选 + 患者姓名展示）+ 新增（患者下拉）+ 编辑 + 取消/开始/完成（按状态禁用）+ 排程子区（新增/删除，按申请列出）。 |
| `/images` | 影像归档（图像管理） | 影像检查列表（显示患者/检查/描述/状态，保存/删除）→ 选中后：序列（添加/删除）→ 影像文件（上传到 MinIO、真实缩略图预览、删除）+ 快速预览（原 viewer manifest + 缓存演示，换说法）。 |
| `/reports` | 诊断报告 | 列表（状态筛选）+ 新增（申请/影像下拉）+ 编辑（草稿/驳回）+ 删除（仅草稿/驳回）+ 提交/审核/驳回（带原因）/回到草稿/发布（按状态禁用）+ 审核日志时间线。 |
| `/settings` | 系统设置 | 报告水印 + 允许下载影像开关 + 重新读取设置（原 Nacos 动态配置）。 |
| `/activity` | 操作记录 | 全局操作日志，用户语言（"已新增患者张明"等）。 |

侧栏 7 项：工作台、患者档案、检查申请、影像归档、诊断报告、系统设置、操作记录。顶栏只留系统名 + 当前用户 + 退出登录（移除网关/Redis/Nacos/Feign 状态标签与 Token 显示）。

## 3. 用户语言改写规则

| 现界面 | 改为 |
|---|---|
| Study / MRI Study / Study UID / Study ID | 影像检查 / 影像编号（UID 对用户隐藏） |
| Series / Series 名称 / Series ID | 检查序列 / 序列名称（ID 隐藏） |
| Image 文件 / 校验值 | 影像文件（校验值隐藏） |
| 归档Study / 新增Series / 登记文件 | 保存影像 / 添加序列 / 上传影像 |
| 顶栏 网关8080/Redis缓存/Nacos注册配置/Feign远程调用 | 全部移除 |
| 分布式流程 | 检查流程：登记→申请→检查→归档→报告 |
| Bearer Token / token 黑名单 | 移除显示；退出提示「已安全退出」 |
| 缓存/配置演示 / Redis查询 / 动态配置 | 影像预览 / 快速预览 / 系统设置 |
| 表单 患者ID/申请ID/Study ID/Series ID（手填） | 下拉选择 |
| 操作日志「网关请求成功」「/api/** 访问网关」 | 「操作成功」「系统已就绪」 |

## 4. 删除功能映射

| 记录 | 前端按钮 | 后端接口 | 说明 |
|---|---|---|---|
| 患者档案 | 删除（确认弹窗 + 依赖提示） | `DELETE /api/patients/{id}` | 有未完成检查时前端警告 |
| 检查申请 | 取消申请 | `POST /api/exams/{id}/cancel` | 后端无硬删除，医疗惯例；加状态守卫 |
| 检查排程 | 删除 | `DELETE /api/exams/schedules/{id}` | |
| 影像检查 | 删除 | `DELETE /api/images/studies/{id}` | 后端级联删序列+文件+MinIO 对象 |
| 检查序列 | 删除 | `DELETE /api/images/series/{id}` | 后端级联删文件+MinIO 对象 |
| 影像文件 | 删除 | `DELETE /api/images/files/{id}` | 后端删 MinIO 对象+库 |
| 诊断报告 | 删除（仅草稿/驳回） | `DELETE /api/reports/{id}` | 已发布不可删 |

每条删除带确认弹窗，成功后从列表移除并记一条操作记录。

## 5. MinIO 影像管理

### 5.1 基础设施（docker-compose.yml）

新增 minio 服务：镜像 `minio/minio:latest`，`server /data --console-address ":9001"`，环境 `MINIO_ROOT_USER=mri / MINIO_ROOT_PASSWORD=mri123456`，端口 `9000:9000`、`9001:9001`，卷 `mri-minio-data:/data`。`01-start-infra.ps1` 走 `docker compose up -d` 自动带上。MinIO 控制台 `http://localhost:9001`（mri/mri123456）便于答辩展示已存图像。

### 5.2 后端（mri-image-service）

- `pom.xml` 加 `io.minio:minio`；根 `pom.xml` dependencyManagement 加 `minio.version=8.5.14`。
- `application.yml` 加 `mri.image.minio.{endpoint=http://localhost:9000,access-key=mri,secret-key=mri123456,bucket=mri-images}` + `spring.servlet.multipart.max-file-size=50MB / max-request-size=50MB`。
- 新增 `MinioImageStorage` 组件：`@PostConstruct` 确保 bucket 存在；`putObject(key, InputStream, contentType, size)`、`getObject(key)`、`removeObject(key)`（对象不存在忽略）。
- `ImageStudyService` 注入 `MinioImageStorage`（4 参构造，更新 `ImageStudyServiceTest` 3 处 mock）：
  - `uploadFile(seriesId, MultipartFile)`：算 SHA-256、对象 key=`series/{seriesId}/{uuid}-{原文件名}`、putObject、`storage_path`=key 入库、清缓存。
  - `streamFile(fileId)`：按 storage_path 从 MinIO 取字节流（含 Content-Type）。
  - `deleteFile` 增强：先 `removeObject(storagePath)`（尽力而为），再删库 + 清缓存。
  - `archive` 增强：要求检查状态为 COMPLETED（经 ExamClient.examStatus）。
- `ExamClient`/`ExamFeignApi`（image 内）：新增 `examStatus(id)` → `GET /api/exams/{id}/status`（exam-service 已有该接口）。
- `ImageStudyController`：
  - 新增 `POST /api/images/studies/{studyId}/files`（`consumes=MULTIPART_FORM_DATA_VALUE`，表单 `seriesId` + `file`），与原 JSON 元数据登记接口同路径不同 Content-Type，互不影响。
  - 新增 `GET /api/images/files/{id}/content` 返回 MinIO 字节流（需 JWT，前端 fetch→blob→objectURL 显示）。
- 新增 2 单测：上传 putObject+入库、删除 removeObject+删库。

### 5.3 网关

`mri-gateway/application.yml` 加 `spring.codec.max-in-memory-size: 50MB`，让大上传通过。`/api/images/**` 路由不变。

### 5.4 前端（/images 页）

- 选影像检查→选序列→「上传影像」文件选择（多选 + 拖拽 + 进度）→ `POST /api/images/studies/{studyId}/files`（FormData：seriesId + file）→ 成功刷新该序列影像列表。
- 影像网格：每张 `fetch GET /api/images/files/{id}/content`（带 JWT）→ blob → objectURL → `<img>` 真实缩略图；种子行（无 MinIO 对象）回退 CSS 占位图。
- 每张图删除按钮（确认→`DELETE /api/images/files/{id}`）。
- 「快速预览」=原 viewer manifest + 缓存演示，并入本页。
- 病人关联：前端用 study→exam→patient 已有数据本地 join，检查卡片显示「患者：张三 / 检查：头颅MRI平扫」。

## 6. 业务逻辑优化

### A. 状态守卫（防越级）
1. 归档影像：要求检查 COMPLETED（image→exam Feign examStatus）。
2. 创建报告：要求检查 COMPLETED + 对应影像已归档（report→exam examStatus + report→image studyExists）。
3. 取消检查：只允许 REQUESTED/IN_PROGRESS（ExamOrderService.cancel 加 requireStatus）。
4. 报告驳回后可回到草稿：新增 `ReportService.reopen(id)` REJECTED→DRAFT + audit；`POST /api/reports/{id}/reopen`。
5. 前端按钮按状态启用/禁用。

### B. 数据模型补全
6. `ExamOrder` record 加 `clinicalDiagnosis`、`priority`（+ toModel）。
7. `Report` record 加 `impression`（+ toModel）。
（grep 全仓 `new ExamOrder(`、`new Report(` 用法并更新受影响测试。）

### C. 级联与一致性
8. `deleteStudy` 级联删其序列 + 文件（含 MinIO 对象）+ 清缓存；`deleteSeries` 级联删其文件（含 MinIO 对象）+ 清缓存。
9. 删除有依赖的患者/检查：前端依赖提示。

### D. 操作方便
10. 下拉选择替代 ID 手填。
11. 联动跳转：建患者→选入检查申请；完成检查→提示去影像归档并预填；归档影像→选入报告表单。
12. 列表搜索/分页/筛选：患者 keyword、检查 status、报告 status（后端已支持）。
13. 工作台待办：待检查/待归档/待审核数量 + 一键跳转带筛选。
14. 影像上传多文件 + 拖拽 + 进度 + 自动刷新。
15. 报告审核显示完整内容 + 驳回原因 + 审核日志时间线（`GET /api/reports/{id}/audit-logs`）。
16. 影像下载带原因 + 下载记录（`POST /api/images/studies/{studyId}/download`、`GET .../download-logs`）。
20. 编辑（PUT，后端已有）：患者（联系信息）、检查申请（临床诊断/优先级）、报告（影像所见/诊断意见，含驳回回到草稿后编辑再提交）可编辑。影像检查/序列/排程编辑不在范围（用删除+重建）。

### E. 已有未开放功能补齐
17. 患者详情：禁忌症增删改（后端已完备）+ 真实检查历史。
   - exam-service 新增 `GET /api/exams/by-patient/{patientId}`（按患者列检查）。
   - patient-service 新增 ExamClient + ExamFeignApi + RemoteExamClient（patient→exam Feign，补全链路），`PatientService.examHistory(patientId)` 调 Feign 返回真实数据（替换写死 stub）。需在 patient-service pom 加 openfeign + loadbalancer，`PatientServiceApplication` 加 `@EnableFeignClients`，controller 改调 service。
18. MRI 安全提示：创建检查时若患者有禁忌症，前端警告。
19. 已发布报告不可删除。

## 7. 前端文件结构

```
mri-frontend/src/
  App.jsx              # 组装路由
  main.jsx / styles.css
  lib/
    api.js             # 补 delete/cancel/reject/reopen/upload(content+multipart)/list-by-patient/audit-logs/download-logs 等
    seed.js            # 保留，按新模型字段补 clinicalDiagnosis/priority/impression
    app-context.jsx    # AppProvider + useApp()（登录态、各列表、操作记录、联动方法）
  components/
    ui.jsx             # Button/TextField/SelectField/TextAreaField/DataTable/StatusTag/SectionHeader/Metric/EmptyState/ConfirmDialog/PageHeader/SearchBar
    AppLayout.jsx      # 侧栏+顶栏外壳 + RequireAuth
  pages/
    LoginPage.jsx  DashboardPage.jsx  PatientsPage.jsx  ExamsPage.jsx
    ImagesPage.jsx  ReportsPage.jsx  SettingsPage.jsx  ActivityPage.jsx
```

`package.json` 加 `react-router-dom`（HashRouter）。

## 8. 后端改动清单（按服务）

- **mri-image-service**：pom 加 minio；application.yml 加 minio+multipart；新增 MinioImageStorage + MinioProperties；ImageStudyService 注入 storage + uploadFile/streamFile + deleteFile removeObject + archive 守卫；ExamClient/ExamFeignApi 加 examStatus；Controller 加 multipart 上传 + content 流式；更新/新增测试。
- **mri-exam-service**：ExamOrder record 加 clinicalDiagnosis/priority + toModel；ExamOrderService.cancel 加守卫；新增 `GET /api/exams/by-patient/{patientId}`（repository.listByPatient + controller）。
- **mri-report-service**：Report record 加 impression + toModel；ReportService.create 加守卫 + 新增 reopen；Controller 加 `POST /api/reports/{id}/reopen`；ExamFeignApi 加 examStatus、ImageClient 加 studyExists；更新测试。
- **mri-patient-service**：pom 加 openfeign+loadbalancer；Application 加 @EnableFeignClients；新增 ExamClient/ExamFeignApi/RemoteExamClient；PatientService.examHistory 调 Feign（替换 stub）；controller 改调 service。
- **mri-gateway**：application.yml 加 `spring.codec.max-in-memory-size: 50MB`。
- **docker-compose.yml**：加 minio 服务 + 卷。
- **docker/mysql/init/01-schema.sql**：不改表结构（复用 storage_path 存对象 key）。

## 9. 文档更新清单

- `README.md`：技术栈加 MinIO；前端界面改页面化 + 登录页 + 上传 + 删除。
- `docs/product-manual.md`：重写「前端界面使用」「功能使用流程」（加 MinIO 上传、删除、患者详情、报告回到草稿）、「常见问题」（加 MinIO 排查）；接口/API/脚本段保留。
- `docs/demo/demo-script.md`：第 1 节加 MinIO；第 11 节重写为登录→各页→上传影像→删除新流程。
- `docs/development-challenges.md`：新增「影像对象存储与 MinIO 集成」难点节；更新第 6 节前端描述。
- `docs/demo/test-result.md`：实现后实跑 `mvn clean test` 与 `npm run build`，据实更新用例数与构建结果（含 MinIO）。
- 开发者向技术段落（技术栈/Swagger/PowerShell 脚本）保留。

## 10. 测试与验证

- 后端：`mvn clean test` 全绿；新增 upload/delete-storage、reopen、cancel 守卫、create 守卫、by-patient 单测；更新受 record arity 变化影响的测试。
- 前端：`npm install && npm run build` 通过，`npm audit` 0 漏洞；Playwright 截图桌面+移动端。
- 联调：docker compose up（含 minio）→ 启服务 → 登录 → 全流程（建患者→禁忌症→申请→排程→开始/完成→归档→上传影像→预览→报告→提交/审核/驳回/回到草稿/发布→下载→删除）。
- 演示脚本 05/07/09（命中 /api/images/studies、/cache-demo、/demo/config、POST /api/images/studies）保持通过。

## 11. 不在范围内 / 注意事项

- 不改检查申请/报告的硬删除语义（检查用取消；报告删除仅限草稿/驳回）。
- 不引入前端额外 UI 框架，沿用现有 styles.css 设计语言扩展。
- MinIO 配置走本地 application.yml（不走 Nacos 动态刷新；水印/下载开关仍走 Nacos）。
- 影像上传走网关（/api），不直连 9004，保持鉴权一致。
- 影像文件表不加 content_type/size 列（Content-Type 由 MinIO 对象自带，流式时透传）。
- 检查历史的 patient→exam Feign 失败时回退空列表（不阻断患者详情）。
