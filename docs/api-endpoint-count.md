# 接口数量说明

系统当前实现 78 个业务与技术验证接口。接口均通过 springdoc-openapi 生成 Swagger/OpenAPI 文档。

| 模块 | 数量 | 说明 |
| --- | ---: | --- |
| Auth/User | 10 | 患者注册、登录、登出、刷新 token、当前用户、用户新增/删除/修改/详情/列表 |
| Patient | 15 | 原患者接口，以及本人档案查询、首次建档、本人修改 |
| Exam | 18 | 原检查与排程接口，以及当前患者本人检查查询 |
| Image | 22 | 原 Study/Series/Image 接口，以及患者本人影像列表、发布后 manifest 和文件读取 |
| Report | 13 | 原报告处理接口，以及患者本人报告进度查询 |
| 合计 | 78 | 高于 50 |

验证脚本：

```powershell
./scripts/demo/04-api-docs-count.ps1
```
