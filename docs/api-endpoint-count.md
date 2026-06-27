# 接口数量说明

系统当前实现 69 个业务/演示接口。接口均通过 springdoc-openapi 生成 Swagger/OpenAPI 文档。

| 模块 | 数量 | 说明 |
| --- | ---: | --- |
| Auth/User | 9 | 登录、登出、刷新 token、当前用户、用户新增/删除/修改/详情/列表 |
| Patient | 12 | 患者 CRUD、分页、MRI 禁忌症 CRUD/列表、检查历史、患者存在性校验 |
| Exam | 17 | 检查申请 CRUD/分页/取消/状态、按患者查询、排程 CRUD/列表、开始/完成、存在性校验、报告状态回写 |
| Image | 19 | Study CRUD/分页、Series CRUD/列表、影像文件 JSON 登记与 multipart 上传/内容读取/详情/删除、viewer manifest、下载、下载日志、缓存演示、动态配置 |
| Report | 12 | 报告 CRUD/分页、提交、审核通过、驳回、回到草稿、发布、按检查单查询、审核日志 |
| 合计 | 69 | 高于 50 |

验证脚本：

```powershell
./scripts/demo/04-api-docs-count.ps1
```
