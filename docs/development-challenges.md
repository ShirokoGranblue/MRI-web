# 开发难点和解决过程

## 1. Spring Boot 3 与 Spring Cloud Alibaba 版本兼容

难点：Spring Boot、Spring Cloud、Spring Cloud Alibaba、Nacos Client 必须匹配，否则容易出现启动失败、配置无法加载或服务无法注册。

解决：采用 Spring Boot 3.5.x、Spring Cloud 2025.0.x、Spring Cloud Alibaba 2025.0.0.0，并统一在根 `pom.xml` 做 BOM 管理。所有服务只声明 starter，不手写传递依赖版本。

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

难点：前端不能做成无关的宣传页，也不能绕过网关直接访问各微服务，否则无法体现登录认证、网关鉴权、Redis/Nacos 演示和远程调用链路；同时界面需对最终用户友好，不能堆砌 Study/Series/UID/Token/Redis/Nacos 等技术术语。

解决：新增 `mri-frontend` React + Vite + react-router 工作台，按功能分页（登录页→工作台→患者档案→检查申请→影像归档→诊断报告→系统设置→操作记录），从登录页进入，未登录自动跳转。所有请求走 `/api/**`，由 Vite 代理到 `http://localhost:8080` 网关。界面使用用户化语言，表单用下拉选择替代手填 ID，记录支持删除（确认弹窗）与编辑，操作按钮按业务状态自动启用/禁用。前端构建使用 Vite 8，`npm audit` 结果为 0 个漏洞。

## 7. 影像对象存储与 MinIO 集成

难点：医生完成 MRI 检查后需上传真实影像文件并按病人浏览，文件不能只存数据库路径字符串，要可预览、可删除、可审计；上传又要经过网关鉴权，不能直连对象存储绕开 JWT。

解决：在 `docker-compose.yml` 增加 MinIO 服务（9000 为对象存储 API，宿主机 9101 映射容器控制台 9001；宿主机 9001 保留给认证服务），`mri-image-service` 引入 MinIO SDK，新增 `MinioImageStorage` 组件在启动时确保 `mri-images` 桶存在。新增 multipart 上传接口 `POST /api/images/studies/{studyId}/files`（表单 `seriesId` + `file`），上传时计算 SHA-256 校验值、生成对象 key（`series/{seriesId}/{uuid}-{文件名}`）写入 MinIO，对象地址存入 `mri_image_file.storage_path`（复用现有列，不改表结构）；新增 `GET /api/images/files/{id}/content` 流式返回影像字节供前端 `<img>` 预览。网关 `spring.codec.max-in-memory-size` 调至 50MB 让大上传通过。删除影像检查/序列/文件时级联清理 MinIO 对象，避免孤儿。前端在「影像归档」页选中影像后可上传、预览真实缩略图、删除，并通过 `cache-demo` 接口演示「快速预览」缓存。

## 8. 医生与患者角色的数据隔离

难点：前端隐藏按钮不能代替后端权限。患者必须只能访问本人资料、检查、影像和报告，同时医生不能修改患者本人资料；报告发布前还要避免正文和影像提前泄露。

解决：网关把鉴权结果区分为公开、已授权、未认证和无权限，并从 JWT 注入可信用户名。各业务服务提供 `/me` 或 `/mine` 接口，所有查询按 `patient.account_username` 过滤；报告服务在未发布时返回空正文，影像服务在读取 manifest 和文件内容前同时检查患者归属与报告发布状态。医生原状态机不变，患者只读接口不接收任意 patientId。

## 9. 全局用户提示与静默实时刷新

难点：原界面只把成功或失败写入操作记录，用户容易错过结果；患者又需要接近实时的进度，但每 5 秒刷新不能不断弹提示或写日志。

解决：在应用 Provider 层增加固定顶部消息条，主动操作默认显示成功或失败；401 清理会话，403 只提示权限不足。患者门户每 5 秒静默请求本人检查、影像和报告，页面隐藏时暂停，恢复时立即刷新，后台刷新设置为不提示、不记录。

## 10. Windows 脚本失败传播与系统中文编码

难点：Windows PowerShell 的 `$ErrorActionPreference` 默认只处理 PowerShell 错误，Docker 等原生程序返回非零时脚本仍可能继续并误报“已清理”。此外，把含中文的 SQL 文本通过 Windows PowerShell 管道传入 MySQL 时，宿主机代码页可能造成 UTF-8 二次编码，使 admin 显示名和系统角色名在页面乱码。

解决：基础设施启动和运行数据清理脚本在每个 Docker/Maven 边界后检查 `$LASTEXITCODE`，Docker 引擎或任一清理步骤失败时立即以非零状态终止。schema 和幂等迁移使用明确的 UTF-8 十六进制 SQL 表达式保存系统中文名称，并在迁移时规范化已有 admin 与角色定义。最终以数据库 `HEX`、字符长度、真实登录响应以及浏览器页面共同验证“系统管理员”和角色名称正确显示。

## 11. Study ZIP 流式生成与成功下载审计

难点：整组影像可能包含多个 Series 和同名文件，不能先把完整 ZIP 全部加载到内存，也不能在权限校验或对象读取失败时生成虚假下载记录。

解决：影像服务使用 `StreamingResponseBody` 和 `ZipOutputStream` 直接向 HTTP 响应写入，每个对象由 MinIO 输入流复制到 ZIP entry。entry 使用 `series-{seriesId}/{安全化文件名}`，同一目录内重复名称追加序号。只有全部 entry 写入并执行 `finish()` 后才插入 `STUDY_ZIP` 下载记录；单文件下载则在对象成功读取后插入 `SINGLE` 记录。日志中的操作人只取网关注入的可信用户名，前端不能提交或覆盖操作人。

## 12. 医生与患者下载访问隔离

难点：患者下载不仅需要角色限制，还要同时满足患者归属和报告发布状态；只隐藏前端按钮无法阻止修改 File ID 或 Study ID 的越权请求。

解决：医生下载使用通用路径，Gateway 拒绝 `PATIENT` 角色访问；患者下载使用 `/api/images/mine/**`，影像服务通过 `patient.account_username` 关联检查归属，并再次检查报告状态为 `PUBLISHED`。`downloadEnabled` 在四个下载入口统一由后端强制校验，前端禁用按钮只用于及时反馈。权限失败、配置关闭、记录不存在或对象读取失败均不写下载日志。

## 13. MRI 风险快照与开始检查前实时复核

难点：只在申请创建时读取禁忌症会形成过期判断，患者可能在排程后新增高风险植入物；患者服务不可用时继续开始检查也不符合安全优先原则。

解决：检查申请创建和修改时由检查服务通过 OpenFeign 读取患者禁忌症，按 `NONE/LOW/HIGH` 规则保存风险级别、摘要和评估时间。未知或空严重程度按 `HIGH` 处理。开始检查时再次读取最新禁忌症并更新评估；高风险未确认返回 409 且保持 `REQUESTED`，确认后记录网关可信医生和确认时间再进入 `IN_PROGRESS`。患者服务异常直接中止状态流转。

## 14. 基于半开区间的排程冲突算法

难点：只比较开始时间无法发现部分重叠和跨检查室的技师冲突，还容易错误禁止首尾相接的排程。

解决：排程增加 `duration_minutes`，服务层把每项排程转换为 `[start, end)`。两个区间在 `leftStart < rightEnd && rightStart < leftEnd` 时重叠；同一检查室或同一非空技师命中即返回 409。更新时排除当前记录，逻辑删除和已取消检查的排程不参与，结束时间等于下一项开始时间时允许保存。前端提供 30、45、60 分钟选项，后端独立校验 15—180 分钟边界。
