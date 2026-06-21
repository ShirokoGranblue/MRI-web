# 演示脚本说明

## 1. 启动中间件

执行 `scripts/demo/01-start-infra.ps1`，启动 MySQL、Redis、Nacos。演示 Nacos 页面：`http://localhost:8848/nacos`。

## 2. 启动微服务

执行 `scripts/demo/02-start-services.ps1`。服务启动后，在 Nacos 服务列表中可以看到：

- `mri-auth-service`
- `mri-patient-service`
- `mri-exam-service`
- `mri-image-service`
- `mri-report-service`
- `mri-gateway`

## 3. 登录认证

执行 `scripts/demo/03-login.ps1`，通过网关访问 `POST /api/auth/login`，默认账号为 `admin/admin123`。脚本会把 JWT 保存到 `target-demo-output-token.txt`。

## 4. 接口文档和接口数量

执行 `scripts/demo/04-api-docs-count.ps1`，脚本读取各服务 `/v3/api-docs` 并统计接口数量，总数应不低于 50。

## 5. Redis 缓存

执行 `scripts/demo/05-redis-cache.ps1`，连续访问 Study 详情和缓存演示接口，并查看 Redis 中 `mri:*` 相关 key。系统代码中患者和 Study 查询均有缓存失效逻辑。

## 6. 注册中心注册与注销

执行 `scripts/demo/06-nacos-register-deregister.ps1`。脚本会读取 `02-start-services.ps1` 保存的 `mri-image-service` PID，展示停止前的 Nacos 服务列表，停止影像服务后展示注销结果，再重启影像服务并展示重新注册结果。

## 7. 远程调用

执行 `scripts/demo/07-feign-remote-call.ps1`：

- `mri-exam-service` 调用 `mri-patient-service` 校验患者。
- `mri-image-service` 调用 `mri-exam-service` 校验检查单。
- `mri-report-service` 调用 `mri-image-service` 读取 Study 信息，并调用 `mri-exam-service` 回写报告状态。

## 8. 网关访问

执行 `scripts/demo/08-gateway-access.ps1`。无 token 访问业务接口返回 401；携带 Bearer token 访问成功。

## 9. 配置中心动态刷新

执行 `scripts/demo/09-nacos-config-refresh.ps1`，修改 `mri-image-service.yaml` 中水印和下载开关，再通过网关访问 `/api/images/demo/config` 查看配置实时变化。

## 10. Git 提交和推送

执行：

```powershell
./scripts/demo/10-git-demo.ps1 -RemoteUrl "你的 GitHub 或 Gitee 仓库地址"
```

脚本会演示 `git init`、`git add`、`git commit`、`git remote add origin` 或 `git remote set-url origin`、`git push`。
## 11. 前端界面演示

前端模块位于 `mri-frontend`，建议在中间件、微服务和网关启动后执行：

```powershell
cd mri-frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。演示顺序建议为：

1. 使用 `admin/admin123` 登录，观察 Bearer Token 状态。
2. 在“患者档案”新增患者并刷新列表。
3. 在“检查申请”创建 MRI 检查申请、安排排程、执行开始和完成检查。
4. 在“影像归档”归档 Study、新增 Series、登记 Image 文件元数据。
5. 在“报告审核”新增报告、提交审核、审核通过、发布报告。
6. 在“缓存配置”读取 viewer manifest、执行 Redis 查询、读取 Nacos 动态配置。
7. 查看右侧操作日志，确认请求均通过 `/api/**` 网关访问。

本地视觉验证截图保存于：

- `target-demo-output/mri-frontend-desktop.png`
- `target-demo-output/mri-frontend-mobile.png`
