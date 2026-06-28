# 测试执行结果

已在本机 Java 21 环境执行全量测试：

```powershell
mvn clean test
```

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

- `npm test`：9 个前端回归测试全部通过，失败 0 个，覆盖失效会话清理、403 保持会话、HTTP 错误用户化、业务提示优先级、网络异常提示、角色会话、患者导航、注册用户名默认行为和首次建档跳转。
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

## 最终零数据与空环境验证

端到端验证结束后执行 `scripts/db/clear-runtime-data.ps1`。脚本实际删除了上传到 `mri-images` bucket 的验证影像，并保留空 bucket。独立查询结果：

- `patient`、`mri_contraindication`、`mri_exam_order`、`mri_schedule`、`mri_study`、`mri_series`、`mri_image_file`、`mri_download_log`、`mri_report`、`mri_report_audit_log`：全部为 0。
- 具有 `PATIENT` 角色的账号：0。
- admin：保留、启用，显示名为“系统管理员”，角色为 `ADMIN`、`RADIOLOGIST`、`AUDITOR`。
- `PATIENT` 角色定义：保留。
- Redis `DBSIZE`：0。
- MinIO `mri-images`：`0 B / 0 objects / 0 versions`，bucket 保留。
- `storage/mri-images`：仅保留 `.gitkeep`。

通过 8080 网关完成不写业务数据的冒烟验证：admin 登录和 `/api/auth/me` 成功；患者、检查、Study、报告四个分页接口均返回 `total: 0, records: []`。浏览器中医生工作台四项统计均为 0，患者页为只读空列表，检查、影像和报告页显示正常空状态；未发现前端运行错误。
