# 测试执行结果

## 后端全量测试

执行命令：

```powershell
mvn clean test
```

执行环境：Java 21、Maven Reactor。

| 模块 | 测试数 | 失败 | 错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: |
| `mri-common` | 3 | 0 | 0 | 0 |
| `mri-auth-service` | 11 | 0 | 0 | 0 |
| `mri-patient-service` | 10 | 0 | 0 | 0 |
| `mri-exam-service` | 29 | 0 | 0 | 0 |
| `mri-image-service` | 32 | 0 | 0 | 0 |
| `mri-report-service` | 12 | 0 | 0 | 0 |
| `mri-gateway` | 6 | 0 | 0 | 0 |
| **合计** | **103** | **0** | **0** | **0** |

最近一次执行结果为 `BUILD SUCCESS`。编译过程中的未检查操作提示和 Java agent 提示属于警告，不计为测试失败。

新增覆盖包括：

- 数据库初始化字段与增强迁移字段检查；
- PBKDF2 密码哈希、随机盐、旧哈希兼容和畸形哈希拒绝；
- `NONE/LOW/HIGH` 风险分级和未知严重程度的安全处理；
- 创建申请保存风险快照；
- 开始检查前重新读取禁忌症；
- 高风险未确认返回 409，高风险确认记录当前医生和时间；
- 患者服务异常时阻止开始检查；
- 检查室时间重叠和技师跨检查室重叠；
- 半开区间首尾相接、更新排除自身、时长边界和非待检查状态；
- 单文件字节、MIME 和原始文件名；
- Study ZIP 的 Series 目录、跨 Series 同名文件和成功后审计；
- 下载关闭、空 Study、对象读取失败不写虚假下载记录；
- 对象读取失败不向用户暴露对象存储路径；
- 患者本人和报告发布门禁；
- 已审核/已发布报告不可修改或删除，审核动作必须记录已认证操作人；
- Gateway 允许患者 `/mine` 下载并拒绝患者通用下载、风险确认和排程写操作。

## 前端轻量回归

正式命令：

```powershell
Set-Location mri-frontend
npm test
```

测试文件也可在受限环境中分别执行：

```powershell
node src/lib/api.test.js
node src/lib/role-utils.test.js
node src/lib/workflow-utils.test.js
```

当前三个测试文件合计 13 项，覆盖：

- 401 清除会话；
- 403 保留会话；
- HTTP、业务错误和网络错误的用户提示；
- 角色会话、患者导航和首次建档跳转；
- 下载请求附带 JWT；
- UTF-8 `Content-Disposition` 文件名解析；
- Blob Object URL 创建、点击和释放；
- 医生与患者下载路径；
- 风险标签映射；
- 排程结束时间计算。

## 前端生产构建

执行命令：

```powershell
Set-Location mri-frontend
npm run build
```

最近一次输出：

```text
vite v8.0.16
1583 modules transformed
dist/index.html                   0.42 kB
dist/assets/index-BJNa2APd.css   15.97 kB
dist/assets/index-BC-g9aeZ.js   239.21 kB
built successfully
```

## 运行态验证项目

运行态验证使用真实 MySQL、Redis、Nacos、MinIO、五个业务服务和 Gateway，按以下顺序执行：

1. 连续执行增强迁移两次并检查字段；
2. 创建患者、禁忌症、检查申请和排程；
3. 验证高风险未确认保持待检查，确认后进入检查中；
4. 验证同检查室冲突、同技师冲突和首尾相接；
5. 上传两个 Series 的同名影像；
6. 验证医生单文件和 Study ZIP 下载；
7. 验证患者本人/他人、发布前/发布后的访问隔离；
8. 查询成功下载记录；
9. 运行 OpenAPI 统计；
10. 执行运行数据清理并独立检查零数据。

本次完整运行结果：

- 两份幂等迁移均连续执行两次成功，8 个增强字段各存在一份；
- 医生单文件下载返回 `image/png` 和 UTF-8 文件名，下载字节的 SHA-256 与源文件一致；
- `Study-1-影像.zip` 可正常打开，包含两个不同 `series-{seriesId}` 目录下的同名文件；
- 发布前、其他患者和 PATIENT 访问医生下载接口均被拒绝，失败请求未写下载记录；
- 浏览器将医生单文件、Study ZIP 和患者本人单文件真实保存到 Windows 下载目录；
- OpenAPI 实际统计为 82 个接口；
- 清理后的零数据结果见 README 的“最终交付状态”。
