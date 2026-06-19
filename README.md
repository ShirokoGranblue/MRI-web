# 医院核磁共振图像信息管理分布式微服务系统

本项目是一套用于课程演示和答辩展示的 Spring Boot 3 分布式微服务系统，主题为医院核磁共振图像信息管理。系统覆盖患者建档、MRI 检查申请、检查排程、影像 Study/Series/Image 元数据归档、影像预览清单、诊断报告审核发布、登录认证、网关访问、Redis 缓存、Nacos 注册发现和配置刷新。

## 技术栈

- Java 21
- Spring Boot 3.3.x
- Spring Cloud 2023.0.x
- Spring Cloud Alibaba Nacos 2023.0.3.4
- MyBatis-Plus 3.5.x
- MySQL 8、Redis 7、Nacos 2.4
- Spring Cloud Gateway、OpenFeign、springdoc-openapi

## 模块

- `mri-common`：统一响应、JWT、密码哈希、异常处理、MyBatis-Plus 配置、OpenAPI 配置。
- `mri-auth-service`：认证、用户、角色。
- `mri-patient-service`：患者档案、MRI 禁忌症、检查历史。
- `mri-exam-service`：MRI 检查申请、排程、状态流转。
- `mri-image-service`：Study、Series、Image 文件、viewer manifest、下载审计。
- `mri-report-service`：诊断报告、审核、发布。
- `mri-gateway`：统一 API 网关、JWT 鉴权、路由。

## 快速运行

```powershell
docker compose up -d mysql redis nacos
mvn -DskipTests install
mvn -pl mri-auth-service spring-boot:run
mvn -pl mri-patient-service spring-boot:run
mvn -pl mri-exam-service spring-boot:run
mvn -pl mri-image-service spring-boot:run
mvn -pl mri-report-service spring-boot:run
mvn -pl mri-gateway spring-boot:run
```

默认账号：

- 用户名：`admin`
- 密码：`admin123`

网关入口：`http://localhost:8080/api/**`

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
