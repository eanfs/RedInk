# RedInk AI 图文生成器 Go后端

基于 CloudWeGo Hertz 框架的 Go 语言后端实现。

## 项目结构

```
go-backend/
├── main.go              # 应用入口
├── api/                 # API 层
│   ├── router.go        # 路由定义
│   └── health.go        # 健康检查API
├── config/              # 配置管理
│   └── config.go        # 配置加载和解析
├── service/             # 服务层
│   ├── init.go          # 服务初始化
│   ├── outline.go       # 大纲生成服务
│   └── ...              # 其他服务
├── generators/          # AI生成器接口和实现
│   ├── base.go          # 生成器基类
│   ├── google_genai.go  # Google Gemini 实现
│   └── ...              # 其他生成器
├── utils/               # 工具函数
│   └── ...
└── go.mod               # 项目依赖
```

## 技术栈

- **Web 框架**: CloudWeGo Hertz (高性能 Go Web 框架)
- **配置管理**: YAML + 环境变量
- **HTTP 客户端**: Resty
- **图像处理**: nfnt/resize
- **日志**: Hertz 内置日志

## 功能模块

1. **大纲生成** (`/api/outline`)
   - 根据用户输入的主题和参考图片生成图文大纲
   - 支持多轮对话优化

2. **图片生成** (`/api/generate`)
   - 根据大纲和参考图片生成多张图片
   - 支持 SSE 流式返回结果
   - 支持重试和重新生成

3. **历史记录** (`/api/history`)
   - 保存和管理生成历史
   - 支持搜索、分页、统计等功能
   - 支持批量下载

4. **配置管理** (`/api/config`)
   - 管理 AI 服务商配置
   - 支持连接测试

## 快速开始

### 1. 安装依赖

```bash
cd go-backend
go mod tidy
```

### 2. 配置文件

在项目根目录创建配置文件：

```bash
cp ../text_providers.yaml.example ../text_providers.yaml
cp ../image_providers.yaml.example ../image_providers.yaml
```

编辑配置文件，填入你的 API Key。

### 3. 启动服务

```bash
go run main.go
```

服务将默认运行在 `http://0.0.0.0:8080`

### 4. 测试服务

```bash
curl http://localhost:8080/api/health
```

## 开发说明

### API 开发流程

1. 在 `api/router.go` 中定义路由
2. 在对应的 API 文件中实现 Handler
3. 在 `service/` 目录中实现业务逻辑
4. 在 `generators/` 目录中实现 AI 生成器

### 生成器扩展

要添加新的 AI 服务商支持：

1. 在 `generators/` 目录中创建新的实现文件
2. 实现 `Generator` 接口
3. 在配置文件中添加服务商配置

## 注意事项

1. **性能优化**: 对于长时间运行的图片生成任务，使用 SSE 流式返回结果
2. **错误处理**: 统一的错误格式和日志记录
3. **安全性**: API Key 等敏感信息应从环境变量或加密配置中加载
4. **兼容性**: 保持与原 Python 后端的 API 格式兼容

## License

MIT