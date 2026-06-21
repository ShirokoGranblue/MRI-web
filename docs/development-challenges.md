# 开发难点和解决过程

## 1. Spring Boot 3 与 Spring Cloud Alibaba 版本兼容

难点：Spring Boot、Spring Cloud、Spring Cloud Alibaba、Nacos Client 必须匹配，否则容易出现启动失败、配置无法加载或服务无法注册。

解决：采用 Spring Boot 3.3.x、Spring Cloud 2023.0.x、Spring Cloud Alibaba 2023.0.3.4，并统一在根 `pom.xml` 做 BOM 管理。所有服务只声明 starter，不手写传递依赖版本。

## 2. Nacos 注册中心的注册与注销演示

难点：注册中心演示不仅要服务能注册，还要能看见服务停止后的注销过程。

解决：所有服务加入 `spring-cloud-starter-alibaba-nacos-discovery` 并配置 `spring.cloud.nacos.discovery.server-addr`。演示时先启动服务观察 Nacos 服务列表，再停止 `mri-image-service` 观察实例下线，最后重启观察重新注册。

## 3. API 网关 JWT 鉴权

难点：登录接口需要放行，业务接口需要鉴权，登出 token 还要失效。

解决：`mri-gateway` 使用 `GatewayAuthFilter` 统一拦截。`/api/auth/login`、Swagger、健康检查为白名单；其他接口必须携带 `Authorization: Bearer <token>`。`mri-auth-service` 登出后将 token 写入 Redis 黑名单，网关用响应式 Redis 查询黑名单后拒绝访问，并对用户管理、报告审核发布接口做基础角色限制。

## 4. MyBatis-Plus 逻辑删除、分页和乐观锁

难点：多服务都要使用统一的数据访问规则，避免每个模块重复配置。

解决：`mri-common` 提供 `MybatisPlusCommonConfig`，统一启用分页插件和乐观锁插件；实体继承 `BaseEntity`，统一包含 `id`、`createdAt`、`updatedAt`、`deleted`、`version`。各服务 Mapper 继承 `BaseMapper<T>`。

## 5. Redis 缓存一致性

难点：患者详情、Study 详情和 viewer manifest 适合缓存，但修改后必须失效，否则演示会出现旧数据。

解决：患者服务在查询详情时写入 Redis key `mri:patient:{id}`，修改/删除患者后清理缓存；影像服务缓存 `mri:study:{studyId}` 和 `mri:viewer:{studyId}`，修改 Study、Series 或 Image 文件后清理相关缓存。viewer manifest 会校验当前水印和下载开关，避免配置中心刷新后继续命中旧配置。演示脚本连续查询并查看 Redis key，展示缓存命中和失效过程。
## 6. 前端工作台与网关联调

难点：系统最初只有后端微服务和脚本演示，缺少面向答辩老师和演示人员的可视化入口。前端不能做成无关的宣传页，也不能绕过网关直接访问各微服务，否则无法体现登录认证、网关鉴权、Redis/Nacos 演示和远程调用链路。

解决：新增 `mri-frontend` React + Vite 工作台，所有请求默认走 `/api/**`，由 Vite 代理到 `http://localhost:8080` 网关。界面只保留 MRI 业务相关内容，包括患者档案、检查申请、检查排程、Study/Series/Image 元数据、viewer manifest、报告审核发布、Redis 缓存和 Nacos 配置演示。前端构建使用 Vite 8，`npm audit` 结果为 0 个漏洞，并通过 Playwright 生成桌面和移动端截图进行可视化检查。
