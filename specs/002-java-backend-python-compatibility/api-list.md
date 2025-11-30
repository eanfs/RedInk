# 需要实现的API接口列表
以下是Java后端需要实现或完善的API接口：

1. **大纲生成接口** - POST /api/outline
2. **图片生成接口** - POST /api/generate
3. **图片下载接口** - GET /api/images/{taskId}/{filename}
4. **单张图片重试接口** - POST /api/retry
5. **批量失败图片重试接口** - POST /api/retry-failed
6. **单张图片重新生成接口** - POST /api/regenerate
7. **任务状态查询接口** - GET /api/task/{taskId}
8. **健康检查接口** - GET /api/health
9. **创建历史记录接口** - POST /api/history
10. **历史记录列表接口** - GET /api/history
11. **历史记录详情接口** - GET /api/history/{recordId}
12. **更新历史记录接口** - PUT /api/history/{recordId}
13. **删除历史记录接口** - DELETE /api/history/{recordId}
14. **历史记录搜索接口** - POST /api/history/search
15. **历史记录统计接口** - GET /api/history/stats
16. **任务扫描接口** - GET /api/task/{taskId}/scan
17. **所有任务扫描接口** - GET /api/task/scan-all
18. **历史记录下载接口** - GET /api/history/{recordId}/zip

# Python后端已实现但Java后端缺少的接口
通过对比，Java后端缺少以下接口：
1. POST /api/retry-failed
2. GET /api/history
3. GET /api/history/{recordId}
4. PUT /api/history/{recordId}
5. DELETE /api/history/{recordId}
6. POST /api/history/search
7. GET /api/history/stats
8. GET /api/task/{taskId}/scan
9. GET /api/task/scan-all
10. GET /api/history/{recordId}/zip
