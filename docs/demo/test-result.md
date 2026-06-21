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

测试用例合计 16 个，失败 0 个，错误 0 个，跳过 0 个。Maven 编译日志显示 `javac [debug release 21]`。覆盖认证登录/登出、患者缓存、检查申请远程校验、影像 Study 缓存和 viewer manifest、报告发布远程协同、网关 JWT 鉴权和基础角色授权。

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
npm run build
npm audit --json
```

执行结果：

- `npm run build`：SUCCESS，Vite 8.0.16 生成 `dist/index.html`、CSS 和 JS 资源。
- `npm audit --json`：漏洞数量为 0。
- Playwright 截图验证：已生成桌面和移动端截图，文件位于 `target-demo-output/mri-frontend-desktop.png` 和 `target-demo-output/mri-frontend-mobile.png`。
