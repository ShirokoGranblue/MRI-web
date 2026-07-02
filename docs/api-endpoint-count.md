# 接口数量说明

系统接口由 springdoc-openapi 根据控制器映射生成。统计单位为 OpenAPI `paths` 中的 HTTP operation，不把同一路径和方法的不同请求体类型重复计数。

| 模块 | 数量 | 主要接口范围 |
| --- | ---: | --- |
| Auth/User | 10 | 登录、注册、退出、刷新、当前用户、用户管理 |
| Patient | 15 | 患者资料、本人资料、MRI 禁忌症、检查历史 |
| Exam | 19 | 检查申请、本人检查、风险复核、状态流转、排程 |
| Image | 25 | Study、Series、文件、预览、医生与患者下载、下载记录 |
| Report | 13 | 报告编写、审核、发布、本人报告、审核记录 |
| **合计** | **82** | 五个业务服务 |

增强功能增加四项可访问能力：

- `GET /exams/{id}/risk`
- `GET /images/files/{fileId}/download`
- `GET /images/mine/files/{fileId}/download`
- `GET /images/mine/studies/{studyId}/download`

Study ZIP 使用既有 Study 下载路径的 `GET` operation，因此影像服务的 OpenAPI operation 净增 3 项，检查服务净增 1 项。

运行态统计命令：

```powershell
./scripts/demo/04-api-docs-count.ps1
```

脚本依次读取：

```text
http://localhost:9001/v3/api-docs
http://localhost:9002/v3/api-docs
http://localhost:9003/v3/api-docs
http://localhost:9004/v3/api-docs
http://localhost:9005/v3/api-docs
```

文档中的分项和总数必须与该脚本最后一次执行输出保持一致。
