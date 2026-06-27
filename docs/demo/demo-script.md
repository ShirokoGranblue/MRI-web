# 演示脚本说明

正式录制系统验证视频时，除操作步骤外，还需要通过字幕讲清楚项目定位、业务流程、技术选型、系统架构以及与 10 项技术验收指标的对应关系。完整无口播操作脚本见 `docs/demo/video-caption-script.md`。

## 1. 启动中间件

执行 `scripts/demo/01-start-infra.ps1`，启动 MySQL、Redis、Nacos、MinIO。演示 Nacos 页面：`http://localhost:8848/nacos`；MinIO 控制台：`http://localhost:9101`（账号 `mri` / `mri123456`），可查看影像对象存储桶 `mri-images`。`9001` 保留给认证服务及其 Swagger。

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

浏览器访问 `http://localhost:5173`。界面按功能分页、使用用户化语言。演示顺序建议为：

1. 在登录页使用 `admin/admin123` 登录，进入工作台查看数量概览、检查流程与待办。
2. 在「患者档案」搜索/新增患者；点击「详情」登记 MRI 禁忌症并查看检查历史；练习编辑与删除（确认弹窗）。
3. 在「检查申请」下拉选患者创建申请；排程子区新增排程；执行开始、完成检查；尝试取消（仅待检查/进行中可用，验证状态守卫）；练习删除（随时可删，连带排程）。
4. 在「影像归档」选择已完成的检查保存影像；选中影像后添加序列、上传影像文件（存入 MinIO）、查看真实缩略图预览、删除影像；点击「快速预览」演示缓存加载。
5. 在「诊断报告」下拉选检查/影像新增报告；提交审核、审核通过、发布；或驳回后「回到草稿」修改再提交；查看审核日志时间线；练习删除（随时可删，含已发布报告）。
6. 在「系统设置」查看报告水印与下载开关，点击「重新读取设置」刷新。
7. 在「操作记录」查看全部操作的成功与失败记录。
