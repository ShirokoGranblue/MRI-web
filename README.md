# 医院核磁共振图像信息管理分布式微服务系统

本项目是一套基于 Spring Boot 3 的医院核磁共振图像信息管理分布式微服务系统。系统覆盖患者自助建档（含 MRI 禁忌症）、医生检查申请与排程、影像 Study/Series/Image 元数据归档、基于 MinIO 的影像对象存储与上传预览、诊断报告审核发布、患者进度查询、登录认证、角色权限、网关访问、Redis 缓存、Nacos 注册发现和配置刷新。

## 技术栈

- Java 21
- Spring Boot 3.3.x
- Spring Cloud 2023.0.x
- Spring Cloud Alibaba Nacos 2023.0.3.4
- MyBatis-Plus 3.5.x
- MySQL 8、Redis 7、Nacos 2.4、MinIO（影像对象存储）
- Spring Cloud Gateway、OpenFeign、springdoc-openapi

## 模块

- `mri-common`：统一响应、JWT、密码哈希、异常处理、MyBatis-Plus 配置、OpenAPI 配置。
- `mri-auth-service`：认证、用户、角色。
- `mri-patient-service`：患者档案、MRI 禁忌症、检查历史。
- `mri-exam-service`：MRI 检查申请、排程、状态流转。
- `mri-image-service`：Study、Series、Image 文件、viewer manifest、下载审计、MinIO 对象存储与上传。
- `mri-report-service`：诊断报告、审核、发布。
- `mri-gateway`：统一 API 网关、JWT 鉴权、路由。
- `mri-frontend`：中文前端工作台，通过 API 网关访问各微服务。

## 快速运行

推荐使用脚本后台启动全部后端服务，避免 `spring-boot:run` 在当前终端阻塞后只启动第一个服务：

```powershell
./scripts/demo/01-start-infra.ps1
./scripts/demo/02-start-services.ps1
```

基础设施脚本会同时启动 MySQL、Redis、Nacos 和 MinIO。认证服务及 Swagger 使用 `9001`，MinIO API 使用 `9000`，MinIO 管理控制台使用 `9101`，避免端口冲突。

如果本机 `8080` 已被占用，可把网关临时启动到其他端口，例如 `18080`：

```powershell
./scripts/demo/02-start-services.ps1 -GatewayPort 18080
```

如需手动启动，每个 `spring-boot:run` 命令都要放在单独终端中运行：

```powershell
mvn -DskipTests install
mvn -pl mri-auth-service spring-boot:run
mvn -pl mri-patient-service spring-boot:run
mvn -pl mri-exam-service spring-boot:run
mvn -pl mri-image-service spring-boot:run
mvn -pl mri-report-service spring-boot:run
mvn -pl mri-gateway spring-boot:run
```

默认账号：

- 医生用户名：`admin`
- 密码：`admin123`

患者账号通过登录页的“患者注册”功能创建。注册后患者首次登录自行提交本人档案和有/无 MRI 禁忌症；医生只能查看患者档案，检查、影像和报告业务仍由医生处理。

网关入口：`http://localhost:8080/api/**`

系统启动后业务表为空。需要清空运行验证数据、患者账号、Redis 缓存和 MinIO 对象时，执行 `scripts/db/clear-runtime-data.ps1`；脚本保留 admin 医生账号和系统角色定义。

## 前端界面

前端模块位于 `mri-frontend`，默认通过 Vite 代理访问网关 `http://localhost:8080/api/**`。

```powershell
cd mri-frontend
npm install
npm run dev
```

如果网关使用了非默认端口，例如 `18080`，启动前端前设置代理目标：

```powershell
cd mri-frontend
$env:VITE_API_PROXY_TARGET="http://localhost:18080"
npm run dev
```

浏览器访问：`http://localhost:5173`

前端采用 React + react-router 按功能分页，从登录页进入，使用用户化语言（不暴露 Study/Series/UID/Token/Redis/Nacos 等技术术语）。主要页面：

- **登录页**：医生登录与患者注册入口。
- **医生工作台**：数量概览、检查流程、待办事项和最近操作。
- **医生患者档案**：只读搜索、详情、MRI 禁忌症与检查历史。
- **患者门户**：患者本人维护资料和禁忌症，只读查看检查、影像、报告进度；报告发布后开放报告正文和相关影像。
- **检查申请**：状态筛选、新增（下拉选患者）、取消/开始/完成（按状态启用）、编辑、删除（连带排程）；检查排程子区可新增/删除。
- **影像归档**：影像检查列表（按患者关联）、保存/删除（级联）；选中后管理序列与影像文件，支持上传图像到 MinIO 对象存储、真实缩略图预览、删除，及「快速预览」。
- **诊断报告**：状态筛选、新增（下拉选检查/影像）、编辑、删除（随时可删，含已发布）、提交/审核/驳回/回到草稿/发布（按状态启用）、审核日志时间线。
- **系统设置**：报告水印与影像下载开关（读取配置中心）。
- **操作记录**：全部操作的成功与失败记录。

各服务 Swagger：

- Auth: `http://localhost:9001/swagger-ui.html`
- Patient: `http://localhost:9002/swagger-ui.html`
- Exam: `http://localhost:9003/swagger-ui.html`
- Image: `http://localhost:9004/swagger-ui.html`
- Report: `http://localhost:9005/swagger-ui.html`

## 测试

```powershell
mvn clean test
```

## 演示

按顺序执行 `scripts/demo/` 中的脚本：

```powershell
./scripts/demo/01-start-infra.ps1
./scripts/demo/02-start-services.ps1
./scripts/demo/03-login.ps1
./scripts/demo/04-api-docs-count.ps1
./scripts/demo/05-redis-cache.ps1
./scripts/demo/06-nacos-register-deregister.ps1
./scripts/demo/07-feign-remote-call.ps1
./scripts/demo/08-gateway-access.ps1
./scripts/demo/09-nacos-config-refresh.ps1
./scripts/demo/10-git-demo.ps1 -RemoteUrl "https://github.com/ShirokoGranblue/MRI-web.git"
```

详细说明见 `docs/product-manual.md` 和 `docs/demo/demo-script.md`。
