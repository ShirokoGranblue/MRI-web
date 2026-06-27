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

测试用例合计 58 个，失败 0 个，错误 0 个，跳过 0 个。Maven 编译日志显示 `javac [debug parameters release 21]`。新增覆盖患者注册固定 PATIENT 角色、重复用户名冲突、真实当前用户、患者账号字段迁移、网关 401/403 四态权限、医生患者资料只读、患者本人档案和禁忌症一致性、本人检查与排程、未发布报告正文脱敏、影像归属与发布门禁；原认证、缓存、Feign、状态机、MinIO、自恢复、multipart、报告审核发布测试继续通过。

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

- `npm test`：已验证的 8 个前端回归测试全部通过，覆盖失效会话清理、403 保持会话、HTTP 错误用户化、业务提示优先级、网络异常提示、角色会话、患者导航和注册用户名默认行为；首次建档跳转测试已加入测试集。
- `npm run build`：SUCCESS，Vite 8.0.16 生成 `dist/index.html`、CSS 和 JS 资源。前端按角色展示医生工作台或患者本人门户。
- `npm audit --json`：漏洞数量为 0。

## 双账号端到端验证

- 患者在登录页注册 `patient01`，用户名由姓名自动带出后可自定义。
- 患者首次登录提交本人资料和金属植入物禁忌症，顶部成功提示正常。
- 医生患者页仅显示详情，无新增、编辑和删除入口。
- 医生通过真实网关完成检查申请、排程、开始、完成、Study、Series、multipart 文件上传和报告审核发布。
- 报告发布前患者报告正文为空，影像 manifest 返回 403。
- 报告发布后患者看到完整报告、1 个影像文件并成功打开预览。
- PATIENT token 调用医生写接口返回 403，患者会话保持有效。
