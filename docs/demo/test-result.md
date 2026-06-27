# 测试执行结果

已在本机 Java 21 环境执行全量测试：

```powershell
mvn clean test
```

结果文件：`target/demo/test-result.txt`

执行结果摘要：

| 模块 | 结果 |
| --- | --- |
| `mri-cloud` | SUCCESS |
| `mri-common` | SUCCESS |
| `mri-auth-service` | SUCCESS |
| `mri-patient-service` | SUCCESS |
| `mri-exam-service` | SUCCESS |
| `mri-image-service` | SUCCESS |
| `mri-report-service` | SUCCESS |
| `mri-gateway` | SUCCESS |

测试用例合计 43 个，失败 0 个，错误 0 个，跳过 0 个。Maven 编译日志显示 `javac [debug parameters release 21]`。覆盖认证登录/登出、患者缓存、检查申请远程校验与取消守卫、检查申请删除（含缺失校验）、取消后返回最新状态、影像 Study 缓存与 viewer manifest、影像 MinIO 上传/删除与级联删除、MinIO 桶晚启动自恢复、multipart 参数绑定、归档前检查完成校验、归档自动生成 Study Instance UID、诊断报告创建守卫与驳回回到草稿、患者检查历史 Feign、网关 JWT 鉴权和基础角色授权。

控制台摘要：

```text
Reactor Summary for mri-cloud 1.0.0:
mri-cloud .......................................... SUCCESS
mri-common ......................................... SUCCESS
mri-auth-service ................................... SUCCESS
mri-patient-service ................................ SUCCESS
mri-exam-service ................................... SUCCESS
mri-image-service .................................. SUCCESS
mri-report-service ................................. SUCCESS
mri-gateway ........................................ SUCCESS
BUILD SUCCESS
```
## 前端构建验证

已在 `mri-frontend` 执行：

```powershell
npm install
npm test
npm run build
npm audit --json
```

执行结果：

- `npm test`：4 个前端回归测试全部通过，覆盖失效会话清理、HTTP 错误用户化、业务提示优先级和网络异常提示。
- `npm run build`：SUCCESS，Vite 8.0.16 生成 `dist/index.html`、CSS 和 JS 资源。前端采用 react-router 按功能分页（登录页 + 7 个功能页），`react-router-dom` 依赖新增后漏洞数量仍为 0。
- `npm audit --json`：漏洞数量为 0。
