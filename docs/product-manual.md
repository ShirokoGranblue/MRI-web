# 产品使用说明书

## 系统简介

医院核磁共振图像信息管理系统用于演示 MRI 检查相关信息化流程，覆盖患者建档、MRI 检查申请、检查排程、影像归档、影像预览清单、诊断报告审核发布、影像下载审计、登录认证、服务注册发现、Redis 缓存、API 网关和配置中心动态刷新。

本系统为教学演示版本，不解析真实 DICOM，不对接 PACS、HL7 或 FHIR，不用于真实诊疗环境。

## 用户角色

- 管理员：维护用户、角色、系统演示配置。
- 登记人员：维护患者档案、登记 MRI 禁忌症、创建检查申请。
- 技师：安排检查排程、开始检查、完成检查、归档 MRI Study/Series/Image。
- 诊断医生：查看影像预览清单、编写诊断报告、提交审核。
- 审核医生：审核报告、驳回报告、发布报告。

## 登录说明

默认账号：

- 用户名：`admin`
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

前端界面主要区域：

- 左侧导航：工作台、患者档案、检查申请、影像归档、报告审核、缓存配置。
- 顶部状态：显示网关、Redis、Nacos、Feign 相关状态标签。
- 登录认证：使用默认账号 `admin/admin123` 登录，登录后自动保存 Bearer Token。
- 患者档案：新增患者并刷新患者分页查询结果。
- 检查申请：创建 MRI 检查申请、安排检查排程、开始检查、完成检查。
- 影像归档：归档 Study，新增 Series，登记 Image 文件元数据。
- 报告审核：新增报告、提交审核、审核通过、发布报告。
- 缓存配置：读取 viewer manifest，触发 Redis 缓存演示查询，读取 Nacos 动态配置。
- 操作日志：记录前端通过网关调用接口的成功和失败结果。
## 功能使用流程

1. 患者建档：调用 `POST /api/patients` 创建患者。
2. 禁忌症登记：调用 `POST /api/patients/{patientId}/contraindications` 登记 MRI 禁忌症。
3. 创建检查申请：调用 `POST /api/exams`，系统会远程调用患者服务确认患者存在。
4. 安排检查：调用 `POST /api/exams/schedules`。
5. 开始和完成检查：调用 `POST /api/exams/{id}/start`、`POST /api/exams/{id}/complete`。
6. 归档 Study：调用 `POST /api/images/studies`，系统会远程调用检查服务确认检查单存在。
7. 新增 Series 和 Image：调用 `POST /api/images/studies/{studyId}/series`、`POST /api/images/studies/{studyId}/files`。
8. 查看影像预览清单：调用 `GET /api/images/studies/{studyId}/viewer-manifest`。
9. 编写报告：调用 `POST /api/reports`。
10. 审核发布：调用 `POST /api/reports/{id}/submit`、`POST /api/reports/{id}/approve`、`POST /api/reports/{id}/publish`。
11. 下载影像：调用 `POST /api/images/studies/{studyId}/download`，系统会记录下载审计日志。

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

1. `01-start-infra.ps1`：启动 MySQL、Redis、Nacos。
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

端口占用：检查 `8080`、`9001` 至 `9005`、`8848`、`9848`、`3307`、`6379` 是否被占用。

配置修改未刷新：等待 3 至 5 秒后重新调用接口，确认 Nacos dataId 为 `mri-image-service.yaml`。

## 停止系统

关闭各 Spring Boot 服务进程后执行：

```powershell
docker compose down
```

如需清空数据库、Redis、Nacos 数据：

```powershell
docker compose down -v
```
