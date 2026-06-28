# 使用说明书

## 系统简介

医院核磁共振图像信息管理系统覆盖患者自助建档、MRI 禁忌症、检查申请与排程、影像归档、MinIO 影像上传预览、诊断报告审核发布、患者进度查询、登录认证、服务注册发现、Redis 缓存、API 网关和配置中心动态刷新。关键状态流转具有防越级校验，医生和患者的数据权限由网关及后端本人接口共同控制。

## 用户角色

- 医生（admin）：只读查看患者档案，创建和处理检查申请、排程、影像和诊断报告，并查看操作记录。
- 患者（PATIENT）：通过登录页注册，自行维护本人档案和 MRI 禁忌症，只读查看本人检查、影像和报告进度；报告发布后查看正文和相关影像。

## 登录说明

默认账号：

- 医生用户名：`admin`
- 密码：`admin123`

登录接口：

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

登录成功后返回 JWT。后续请求在 Header 中加入：

```text
Authorization: Bearer <token>
```

登出接口：

```http
POST http://localhost:8080/api/auth/logout
Authorization: Bearer <token>
```

登出后 token 会进入 Redis 黑名单，再次访问业务接口会被网关拒绝。


## 前端界面使用

前端工作台位于 `mri-frontend`，启动后通过 API 网关访问后端接口。

```powershell
cd mri-frontend
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

前端采用 React + react-router 按角色分页。医生导航包含工作台、患者档案、检查申请、影像归档、诊断报告、系统设置和操作记录；患者导航只包含工作台、我的资料、我的检查、我的影像和我的报告。

- **登录页**：医生登录；患者通过“患者注册”创建账号，用户名默认使用姓名并允许自定义。
- **工作台**：数量概览、检查流程示意、待办事项（可点击带筛选跳转）、最近操作记录。
- **医生患者档案**：按姓名/编号搜索并只读查看患者资料、MRI 禁忌症和检查历史。
- **患者本人资料**：首次登录时提交基本资料并选择有/无禁忌症，之后仅患者本人可维护。
- **患者进度页面**：每 5 秒更新本人检查、影像和报告状态；报告发布前隐藏正文并锁定影像，发布后开放。
- **检查申请**：按状态筛选、新增（下拉选患者）、取消/开始/完成（按状态自动启用）、编辑、删除（随时可删，连带删除排程）；下方检查排程子区可新增/删除排程。
- **影像归档**：影像检查列表（按患者关联展示）、保存影像（需检查已完成）、删除（级联删除序列与文件）；选中影像后可添加/删除序列、上传影像文件到 MinIO 对象存储并预览真实缩略图、删除影像、快速预览。
- **诊断报告**：按状态筛选、新增（下拉选检查/影像）、编辑、删除（随时可删，含已发布）、提交/审核/驳回（带原因）/回到草稿/发布（按状态自动启用）、审核日志时间线。
- **系统设置**：查看报告水印与影像下载开关，点击「重新读取设置」刷新配置中心配置。
- **操作记录**：记录全部操作的成功与失败结果，使用用户化语言。

## 功能使用流程

1. 患者注册：`POST /api/auth/register`，服务端固定分配 `PATIENT` 角色。
2. 患者本人建档：首次使用 `POST /api/patients/me`，之后使用 `PUT /api/patients/me`，基本资料与禁忌症在同一事务中保存。
3. 检查历史：`GET /api/patients/{patientId}/exam-history`（患者服务经 Feign 调检查服务 `GET /api/exams/by-patient/{patientId}`，返回真实检查记录）。
4. 创建检查申请：`POST /api/exams`（远程校验患者存在）；仅待检查/进行中可取消 `POST /api/exams/{id}/cancel`。
5. 安排检查：`POST /api/exams/schedules`；可删除 `DELETE /api/exams/schedules/{id}`。
6. 开始与完成检查：`POST /api/exams/{id}/start`、`POST /api/exams/{id}/complete`（状态流转有守卫，不可越级）。
7. 归档影像：`POST /api/images/studies`（校验检查已完成后才可归档）。
8. 添加序列与上传影像：`POST /api/images/studies/{studyId}/series` 新增序列；上传影像到 MinIO 用 multipart `POST /api/images/studies/{studyId}/files`（表单字段 `seriesId` + `file`），数据库保存对象存储地址；读取影像内容 `GET /api/images/files/{id}/content`。
9. 查看影像预览清单：`GET /api/images/studies/{studyId}/viewer-manifest`；快速预览（缓存演示）`GET /api/images/studies/{studyId}/cache-demo`。
10. 编写报告：`POST /api/reports`（校验检查已完成且对应影像已归档）。
11. 审核发布：`POST /api/reports/{id}/submit` 提交、`/approve` 审核通过、`/reject?reason=` 驳回、`/reopen` 回到草稿修改、`/publish` 发布（发布时远程回写检查状态为「已出报告」）。
12. 删除业务记录：医生可按原流程删除检查、排程、影像和报告；医生不能修改或删除患者本人资料。
13. 下载影像：`POST /api/images/studies/{studyId}/download`，系统记录下载审计日志，`GET /api/images/studies/{studyId}/download-logs` 查询下载记录。

## 接口文档使用

各服务 Swagger UI：

- 认证服务：`http://localhost:9001/swagger-ui.html`
- 患者服务：`http://localhost:9002/swagger-ui.html`
- 检查服务：`http://localhost:9003/swagger-ui.html`
- 影像服务：`http://localhost:9004/swagger-ui.html`
- 报告服务：`http://localhost:9005/swagger-ui.html`

在 Swagger UI 中点击 Authorize，填入：

```text
Bearer <登录返回的 token>
```

## 演示操作步骤

按 `scripts/demo/` 下脚本编号顺序执行：

1. `01-start-infra.ps1`：启动 MySQL、Redis、Nacos、MinIO。
2. `02-start-services.ps1`：启动 6 个微服务。
3. `03-login.ps1`：登录并保存 token。
4. `04-api-docs-count.ps1`：统计接口数量。
5. `05-redis-cache.ps1`：演示 Redis 缓存。
6. `06-nacos-register-deregister.ps1`：停止并重启影像服务，演示注册中心注册和注销。
7. `07-feign-remote-call.ps1`：演示多个服务之间远程调用。
8. `08-gateway-access.ps1`：演示网关鉴权。
9. `09-nacos-config-refresh.ps1`：演示配置中心动态刷新。
10. `10-git-demo.ps1`：演示 Git 提交和推送。

## 常见问题

Nacos 未启动：执行 `docker compose ps` 检查 `mri-nacos`，必要时执行 `docker compose logs nacos` 查看日志。

Redis 未连接：确认 `mri-redis` 容器运行，端口为 `6379`。

登录 401：确认请求路径为 `http://localhost:8080/api/auth/login`，并且账号密码为 `admin/admin123`。

业务接口 401：确认 Header 中有 `Authorization: Bearer <token>`，且 token 未登出。

服务未注册：确认服务启动时能访问 `localhost:8848`，并在 Nacos 服务列表查看实例。

接口文档打不开：确认对应服务端口已启动，例如影像服务端口为 `9004`。

端口占用：检查 `8080`、`9000`、`9001` 至 `9005`、`9101`、`8848`、`9848`、`3307`、`6379` 是否被占用。`9001` 为认证服务，`9101` 为 MinIO 控制台。

配置修改未刷新：等待 3 至 5 秒后重新调用接口，确认 Nacos dataId 为 `mri-image-service.yaml`。

影像上传失败/预览无图：执行 `docker compose ps` 确认 `mri-minio` 容器运行，端口为 `9000`（API）与 `9101`（控制台宿主机端口）。MinIO 控制台 `http://localhost:9101`（账号 `mri` / `mri123456`）可查看 `mri-images` 桶内已上传对象。`9001` 保留给认证服务及其 Swagger。首次启动时影像服务会自动创建 `mri-images` 桶。

## 停止系统

关闭各 Spring Boot 服务进程后执行：

```powershell
docker compose down
```

如需完全删除本地基础设施数据卷并重新初始化，可执行下列命令。该操作会同时删除账号和系统配置，不用于最终交付清理：

```powershell
docker compose down -v
```

最终交付前清空运行验证数据，同时保留 admin、系统角色定义、Nacos 配置和空 MinIO bucket：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/db/clear-runtime-data.ps1
```

脚本会依次清空业务表、删除所有 PATIENT 账号、清空项目 Redis、删除 `mri-images` bucket 内对象并保留 bucket，以及清理 `storage/mri-images` 运行文件。Docker 引擎不可用或任一步失败时脚本返回非零，不会误报成功。

清理后必须独立确认：

```powershell
docker exec mri-mysql mysql -uroot -proot123456 -D mri_cloud -e "
SELECT COUNT(*) FROM patient;
SELECT COUNT(*) FROM mri_contraindication;
SELECT COUNT(*) FROM mri_exam_order;
SELECT COUNT(*) FROM mri_schedule;
SELECT COUNT(*) FROM mri_study;
SELECT COUNT(*) FROM mri_series;
SELECT COUNT(*) FROM mri_image_file;
SELECT COUNT(*) FROM mri_download_log;
SELECT COUNT(*) FROM mri_report;
SELECT COUNT(*) FROM mri_report_audit_log;
SELECT username, display_name, enabled FROM sys_user WHERE username='admin';
SELECT role_code, role_name FROM sys_role ORDER BY id;"

docker exec mri-redis redis-cli DBSIZE
```

MinIO 使用 `mc stat` 和 `mc du` 验证 `mri-images` bucket 存在且为 `0 objects`。最后只执行 admin 登录、`/api/auth/me` 和空列表查询，不再注册患者或创建业务记录。
